/// Genera y comparte la imagen de "pago móvil" con QR + montos.
///
/// Port de PaymentShareHelper.java: guarda el QR del banco elegido por el
/// usuario, compone una imagen 800px con gradiente oscuro/dorado y la
/// comparte por el sistema (WhatsApp incluido).
library;

import 'dart:io';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';

import '../utils/number_format.dart';

const String _kQrFilename = 'saved_qr.png';
const String _kShareFilename = 'pago_movil.png';

class QrShareService {
  Future<File> _qrFile() async {
    final dir = await getApplicationDocumentsDirectory();
    return File('${dir.path}/qr_images/$_kQrFilename');
  }

  /// Verifica si ya hay un QR guardado.
  Future<bool> hasQrSaved() async => (await _qrFile()).exists();

  /// Guarda la imagen QR seleccionada por el usuario.
  Future<bool> saveQrImage(String imagePath) async {
    try {
      final File source = File(imagePath);
      if (!source.existsSync()) return false;

      final File dest = await _qrFile();
      await dest.parent.create(recursive: true);
      await source.copy(dest.path);
      return true;
    } catch (_) {
      return false;
    }
  }

  Future<ui.Image?> _loadSavedQr() async {
    final File qrFile = await _qrFile();
    if (!qrFile.existsSync()) return null;
    final Uint8List bytes = await qrFile.readAsBytes();
    final ui.Codec codec = await ui.instantiateImageCodec(bytes);
    final ui.FrameInfo frame = await codec.getNextFrame();
    return frame.image;
  }

  /// Genera la imagen compuesta y la comparte. Devuelve null si todo fue
  /// bien, o un mensaje de error.
  Future<String?> generateAndShare({
    required String montoTop,
    required String currencyTop,
    required String montoBottom,
    required String currencyBottom,
    required double tasa,
    required String tasaLabel,
  }) async {
    try {
      final ui.Image? qrImage = await _loadSavedQr();
      if (qrImage == null) return 'No se encontró imagen QR guardada';

      final Uint8List? pngBytes = await _createCompositeImage(
        qrImage,
        montoTop: montoTop,
        currencyTop: currencyTop,
        montoBottom: montoBottom,
        currencyBottom: currencyBottom,
        tasa: tasa,
        tasaLabel: tasaLabel,
      );
      qrImage.dispose();
      if (pngBytes == null) return 'Error generando imagen';

      final dir = await getTemporaryDirectory();
      final shareDir = Directory('${dir.path}/shared_images')
        ..createSync(recursive: true);
      final shareFile = File('${shareDir.path}/$_kShareFilename');
      await shareFile.writeAsBytes(pngBytes);

      await Share.shareXFiles([XFile(shareFile.path)],
          text: 'Datos de pago móvil');
      return null;
    } catch (e) {
      return 'Error al compartir: $e';
    }
  }

  /// Crea la imagen compuesta con el diseño oscuro/dorado del original.
  Future<Uint8List?> _createCompositeImage(
    ui.Image qrImage, {
    required String montoTop,
    required String currencyTop,
    required String montoBottom,
    required String currencyBottom,
    required double tasa,
    required String tasaLabel,
  }) async {
    const double width = 800;
    const double qrSize = 500;
    const double padding = 40;

    const double headerHeight = 120;
    const double qrSectionHeight = qrSize + 40;
    const double amountsHeight = 200;
    const double footerHeight = 80;
    const double totalHeight =
        headerHeight + qrSectionHeight + amountsHeight + footerHeight + padding * 2;

    final recorder = ui.PictureRecorder();
    final canvas = Canvas(recorder);

    // Fondo con gradiente oscuro.
    final bgPaint = Paint()
      ..shader = ui.Gradient.linear(
        Offset.zero,
        const Offset(0, totalHeight),
        const [Color(0xFF121226), Color(0xFF1A1A3E)],
      );
    canvas.drawRect(const Rect.fromLTWH(0, 0, width, totalHeight), bgPaint);

    // Borde dorado sutil.
    final borderPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3
      ..color = const Color(0xFFC9A961);
    canvas.drawRRect(
      RRect.fromRectAndRadius(
        const Rect.fromLTWH(8, 8, width - 16, totalHeight - 16),
        const Radius.circular(20),
      ),
      borderPaint,
    );

    double y = padding;

    // Header.
    _drawText(canvas, 'DATOS DE PAGO MÓVIL', width / 2, y + 40,
        fontSize: 32, color: const Color(0xFFD4AF37), bold: true, center: true);
    _drawText(canvas, 'Escanea el QR para pagar', width / 2, y + 70,
        fontSize: 18, color: const Color(0xFFA0A0A0), center: true);

    // Línea separadora dorada.
    final linePaint = Paint()
      ..color = const Color(0xFFC9A961)
      ..strokeWidth = 1.5;
    y += headerHeight;
    canvas.drawLine(Offset(padding, y), Offset(width - padding, y), linePaint);

    // QR sobre fondo blanco.
    y += 20;
    final qrBgPaint = Paint()..color = const Color(0xFFFFFFFF);
    final double qrX = (width - qrSize - 20) / 2;
    canvas.drawRRect(
      RRect.fromRectAndRadius(
        Rect.fromLTWH(qrX, y, qrSize + 20, qrSize + 20),
        const Radius.circular(12),
      ),
      qrBgPaint,
    );
    canvas.drawImageRect(
      qrImage,
      Rect.fromLTWH(0, 0, qrImage.width.toDouble(), qrImage.height.toDouble()),
      Rect.fromLTWH(qrX + 10, y + 10, qrSize, qrSize),
      Paint(),
    );

    y += qrSize + 40;
    canvas.drawLine(Offset(padding, y), Offset(width - padding, y), linePaint);
    y += 20;

    // Montos.
    _drawText(
      canvas,
      '${currencySymbol(currencyTop)} $montoTop ${currencyCode(currencyTop)}',
      padding + 10,
      y + 40,
      fontSize: 36,
      color: const Color(0xFFFFFFFF),
      bold: true,
    );
    y += 60;
    _drawText(
      canvas,
      '${currencySymbol(currencyBottom)} $montoBottom ${currencyCode(currencyBottom)}',
      padding + 10,
      y + 40,
      fontSize: 36,
      color: const Color(0xFFFFFFFF),
      bold: true,
    );
    y += 60;

    // Tasa.
    if (tasa > 0) {
      _drawText(
        canvas,
        '$tasaLabel: ${tasa.toStringAsFixed(2)} Bs/\$',
        width / 2,
        y + 20,
        fontSize: 20,
        color: const Color(0xFFC9A961),
        center: true,
      );
    }

    // Footer.
    _drawText(
      canvas,
      "Don't look at me 🔒",
      width / 2,
      totalHeight - 50,
      fontSize: 16,
      color: const Color(0xFF505050),
      italic: true,
      center: true,
    );

    final ui.Picture picture = recorder.endRecording();
    final ui.Image image =
        await picture.toImage(width.toInt(), totalHeight.toInt());
    picture.dispose();
    final ByteData? byteData =
        await image.toByteData(format: ui.ImageByteFormat.png);
    image.dispose();
    return byteData?.buffer.asUint8List();
  }

  void _drawText(
    Canvas canvas,
    String text,
    double x,
    double y, {
    required double fontSize,
    required Color color,
    bool bold = false,
    bool italic = false,
    bool center = false,
  }) {
    final builder = ui.ParagraphBuilder(
      ui.ParagraphStyle(textAlign: center ? TextAlign.center : TextAlign.left),
    )
      ..pushStyle(ui.TextStyle(
        color: color,
        fontSize: fontSize,
        fontWeight: bold ? FontWeight.bold : FontWeight.normal,
        fontStyle: italic ? FontStyle.italic : FontStyle.normal,
      ))
      ..addText(text);
    final paragraph = builder.build()..layout(const ui.ParagraphConstraints(width: 720));
    final double dx = center ? x - paragraph.width / 2 : x;
    canvas.drawParagraph(paragraph, Offset(dx, y - fontSize / 2));
    paragraph.dispose();
  }
}
