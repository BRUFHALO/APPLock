/// Calculadora multimoneda.
///
/// Port de CalculatorActivity.java + activity_calculator.xml: conversión en
/// tiempo real USD/EUR/VES/USDT con tasas BCV y Binance P2P, y compartir los
/// datos de pago por WhatsApp con una imagen QR.
library;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:image_cropper/image_cropper.dart';
import 'package:image_picker/image_picker.dart';

import '../services/bcv_service.dart';
import '../services/binance_service.dart';
import '../services/qr_share_service.dart';
import '../services/widget_service.dart';
import '../utils/number_format.dart';
import '../widgets/data_portal.dart';

const Color _kGold = Color(0xFFD4AF37);
const Color _kGoldBright = Color(0xFFE6C163);
const Color _kGoldMuted = Color(0xFFA89060);
const Color _kCardBg = Color(0xFF31312F);
const Color _kDropdownBg = Color(0xFF2D2F3C);
const Color _kSeparator = Color(0xFF444444);
const Color _kError = Color(0xFFFF5252);
const Color _kText = Color(0xFFEDEDED);

class CalculatorScreen extends StatefulWidget {
  const CalculatorScreen({super.key});

  @override
  State<CalculatorScreen> createState() => _CalculatorScreenState();
}

class _CalculatorScreenState extends State<CalculatorScreen> {
  final TextEditingController _inputTop = TextEditingController();
  final TextEditingController _inputBottom = TextEditingController();
  final ImagePicker _picker = ImagePicker();

  final BcvService _bcvService = BcvService();
  final BinanceService _binanceService = BinanceService();
  final QrShareService _shareService = QrShareService();

  int _currencyTop = kUsd;
  int _currencyBottom = kVes;

  double _tasaDolar = 0;
  double _tasaEuro = 0;
  double _tasaUsdt = 0;

  bool _isUpdating = false;

  // Estado UI BCV.
  BcvData? _bcvData;
  bool _bcvLoading = false;
  bool _bcvError = false;
  String _bcvErrorMsg = '';

  // Estado UI Binance.
  BinanceData? _binanceData;
  bool _binanceLoading = false;
  bool _binanceError = false;
  String _binanceErrorMsg = '';

  bool _hasQr = false;

  @override
  void initState() {
    super.initState();
    _inputTop.addListener(_convertFromTop);
    _inputBottom.addListener(_convertFromBottom);
    _fetchBcvData();
    _fetchBinanceData();
    _shareService
        .hasQrSaved()
        .then((has) => mounted ? setState(() => _hasQr = has) : null);
  }

  @override
  void dispose() {
    _inputTop.dispose();
    _inputBottom.dispose();
    super.dispose();
  }

  // ========== Conversión ==========

  void _convertFromTop() {
    if (_isUpdating) return;
    _isUpdating = true;
    try {
      final text = _inputTop.text.trim();
      if (text.isEmpty) {
        _inputBottom.text = '';
        return;
      }
      final result = convert(
        parseFormattedNumber(text),
        _currencyTop,
        _currencyBottom,
        tasaDolar: _tasaDolar,
        tasaEuro: _tasaEuro,
        tasaUsdt: _tasaUsdt,
      );
      if (!result.isNaN && result >= 0) {
        _inputBottom.text = formatResult(result);
      } else {
        _inputBottom.text = '';
      }
    } on FormatException {
      _inputBottom.text = '';
    } finally {
      _isUpdating = false;
    }
  }

  void _convertFromBottom() {
    if (_isUpdating) return;
    _isUpdating = true;
    try {
      final text = _inputBottom.text.trim();
      if (text.isEmpty) {
        _inputTop.text = '';
        return;
      }
      final result = convert(
        parseFormattedNumber(text),
        _currencyBottom,
        _currencyTop,
        tasaDolar: _tasaDolar,
        tasaEuro: _tasaEuro,
        tasaUsdt: _tasaUsdt,
      );
      if (!result.isNaN && result >= 0) {
        _inputTop.text = formatResult(result);
      } else {
        _inputTop.text = '';
      }
    } on FormatException {
      _inputTop.text = '';
    } finally {
      _isUpdating = false;
    }
  }

  void _swapValues() {
    final topValue = _inputTop.text;
    final topCurrency = _currencyTop;
    setState(() {
      _isUpdating = true;
      _inputTop.text = _inputBottom.text;
      _inputBottom.text = topValue;
      _currencyTop = _currencyBottom;
      _currencyBottom = topCurrency;
      _isUpdating = false;
    });
  }

  void _copyToClipboard(String value, String label) {
    if (value.isEmpty || value == '0.00') {
      _toast('No hay valor para copiar');
      return;
    }
    Clipboard.setData(ClipboardData(text: value));
    _toast('✓ Copiado: $value');
  }

  // ========== Tasas ==========

  Future<void> _fetchBcvData({bool force = false}) async {
    setState(() {
      _bcvLoading = true;
      _bcvError = false;
    });
    final result = await _bcvService.obtenerTasas(force: force);
    if (!mounted) return;
    setState(() {
      _bcvLoading = false;
      if (result.isSuccess) {
        _bcvData = result.data;
        _tasaDolar = result.data!.dolar;
        _tasaEuro = result.data!.euro;
      } else if (result.cachedData != null) {
        _bcvData = result.cachedData;
        _tasaDolar = result.cachedData!.dolar;
        _tasaEuro = result.cachedData!.euro;
        _toast('${result.error} (mostrando caché)');
      } else {
        _bcvData = null;
        _bcvError = true;
        _bcvErrorMsg = result.error ?? 'Error';
        _toast(_bcvErrorMsg);
      }
    });
    _convertFromTop();
  }

  Future<void> _fetchBinanceData({bool force = false}) async {
    setState(() {
      _binanceLoading = true;
      _binanceError = false;
    });
    final result = await _binanceService.fetchUsdtVesPrice(force: force);
    if (!mounted) return;
    setState(() {
      _binanceLoading = false;
      if (result.isSuccess) {
        _binanceData = result.data;
        _tasaUsdt = result.data!.price;
      } else if (result.cachedData != null) {
        _binanceData = result.cachedData;
        _tasaUsdt = result.cachedData!.price;
        _toast('${result.error} (mostrando caché)');
      } else {
        _binanceData = null;
        _binanceError = true;
        _binanceErrorMsg = result.error ?? 'Error';
        _toast(_binanceErrorMsg);
      }
    });
    _convertFromTop();
    pushRatesToWidget(
        dolar: _tasaDolar, euro: _tasaEuro, usdt: _tasaUsdt);
  }

  // ========== Compartir ==========

  Future<void> _pickQrImage() async {
    try {
      final XFile? image =
          await _picker.pickImage(source: ImageSource.gallery);
      if (image == null) return;

      final CroppedFile? cropped = await ImageCropper().cropImage(
        sourcePath: image.path,
        uiSettings: [
          AndroidUiSettings(
            toolbarTitle: 'Recortar QR',
            toolbarColor: _kCardBg,
            toolbarWidgetColor: _kGold,
            statusBarLight: false,
            activeControlsWidgetColor: _kGold,
            backgroundColor: Colors.black,
            cropGridColor: const Color(0x55D4AF37),
            cropFrameColor: _kGold,
            lockAspectRatio: false,
          ),
          IOSUiSettings(
            title: 'Recortar QR',
            doneButtonTitle: 'Listo',
            cancelButtonTitle: 'Cancelar',
          ),
        ],
      );
      if (cropped == null) return;

      final saved = await _shareService.saveQrImage(cropped.path);
      if (!mounted) return;
      if (saved) {
        setState(() => _hasQr = true);
        _toast('✅ QR guardado correctamente');
      } else {
        _toast('Error al guardar el QR');
      }
    } catch (e) {
      _toast('Error al seleccionar imagen: $e');
    }
  }

  Future<void> _handleShareClick() async {
    if (!await _shareService.hasQrSaved()) {
      _toast('Primero selecciona la imagen QR de tu banco');
      await _pickQrImage();
      return;
    }

    final montoTop = _inputTop.text.trim();
    final montoBottom = _inputBottom.text.trim();
    if (montoTop.isEmpty && montoBottom.isEmpty) {
      _toast('Ingresa un monto para compartir');
      return;
    }

    final bool hasUsdt = _currencyTop == kUsdt || _currencyBottom == kUsdt;
    final double tasaToUse = hasUsdt ? _tasaUsdt : _tasaDolar;
    final String tasaLabel = hasUsdt ? 'Tasa Binance' : 'Tasa BCV';

    final error = await _shareService.generateAndShare(
      montoTop: montoTop,
      currencyTop: kCurrencies[_currencyTop],
      montoBottom: montoBottom,
      currencyBottom: kCurrencies[_currencyBottom],
      tasa: tasaToUse,
      tasaLabel: tasaLabel,
    );
    if (error != null) _toast(error);
  }

  void _toast(String msg) {
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(msg)));
  }

  // ========== UI ==========

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: SafeArea(
        child: Stack(
          children: [
            const Positioned(
              top: 120,
              left: 0,
              right: 0,
              bottom: 0,
              child: DataPortal(),
            ),
            SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(24, 32, 24, 16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    'Calculadora Multimoneda',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 26,
                      fontWeight: FontWeight.bold,
                      fontFamily: 'serif',
                      color: _kGold,
                      letterSpacing: 0.8,
                    ),
                  ),
                  const SizedBox(height: 4),
                  const Text(
                    'Conversión de Divisas Globales',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 13, color: _kGoldMuted),
                  ),
                  const SizedBox(height: 28),
                  _buildInputCard(
                    controller: _inputTop,
                    currency: _currencyTop,
                    onCurrencyChanged: (v) {
                      setState(() => _currencyTop = v);
                      _convertFromTop();
                    },
                    onCopy: () =>
                        _copyToClipboard(_inputTop.text, 'TENGO'),
                  ),
                  _buildSwapButton(),
                  _buildInputCard(
                    controller: _inputBottom,
                    currency: _currencyBottom,
                    onCurrencyChanged: (v) {
                      setState(() => _currencyBottom = v);
                      _convertFromTop();
                    },
                    onCopy: () =>
                        _copyToClipboard(_inputBottom.text, 'QUIERO'),
                  ),
                  const SizedBox(height: 28),
                  _buildBcvCard(),
                  const SizedBox(height: 24),
                  _buildBinanceCard(),
                  const SizedBox(height: 16),
                  _buildShareButton(),
                  if (_hasQr)
                    TextButton(
                      onPressed: _pickQrImage,
                      child: const Text(
                        '🔄 CAMBIAR QR DEL BANCO',
                        style: TextStyle(fontSize: 12, color: _kGoldMuted),
                      ),
                    ),
                  const SizedBox(height: 8),
                  const Text(
                    'Desarrollado por BRUFHALO',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 11,
                      fontStyle: FontStyle.italic,
                      color: Color(0x44D4AF37),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildInputCard({
    required TextEditingController controller,
    required int currency,
    required ValueChanged<int> onCurrencyChanged,
    required VoidCallback onCopy,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: _kCardBg,
        borderRadius: BorderRadius.circular(20),
      ),
      padding: const EdgeInsets.all(16),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Expanded(
            child: TextField(
              controller: controller,
              keyboardType:
                  const TextInputType.numberWithOptions(decimal: true),
              maxLines: 1,
              style: const TextStyle(
                fontSize: 24,
                fontWeight: FontWeight.w300,
                color: Colors.white,
              ),
              decoration: const InputDecoration(
                hintText: '0.00',
                hintStyle: TextStyle(color: Color(0x55FFFFFF)),
                border: InputBorder.none,
                isDense: true,
                contentPadding: EdgeInsets.symmetric(vertical: 8),
              ),
            ),
          ),
          const SizedBox(width: 8),
          Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              InkWell(
                onTap: onCopy,
                child: const Padding(
                  padding: EdgeInsets.all(4),
                  child: Text('📋', style: TextStyle(fontSize: 13)),
                ),
              ),
              const SizedBox(height: 4),
              Container(
                width: 100,
                height: 40,
                decoration: BoxDecoration(
                  border: Border.all(color: _kGoldBright, width: 1.5),
                  borderRadius: BorderRadius.circular(12),
                ),
                padding: const EdgeInsets.symmetric(horizontal: 8),
                child: DropdownButtonHideUnderline(
                  child: DropdownButton<int>(
                    value: currency,
                    isExpanded: true,
                    dropdownColor: _kDropdownBg,
                    iconEnabledColor: _kGoldBright,
                    style: const TextStyle(fontSize: 14, color: _kGoldBright),
                    items: [
                      for (int i = 0; i < kCurrencies.length; i++)
                        DropdownMenuItem(
                          value: i,
                          child: Center(child: Text(kCurrencies[i])),
                        ),
                    ],
                    onChanged: (v) {
                      if (v != null) onCurrencyChanged(v);
                    },
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildSwapButton() {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Center(
        child: InkWell(
          onTap: _swapValues,
          customBorder: const CircleBorder(),
          child: Container(
            width: 52,
            height: 52,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: const Color(0xFF232538),
              border: Border.all(color: _kGold, width: 1.5),
            ),
            child: const Center(
              child: Text(
                '⇅',
                style: TextStyle(fontSize: 24, color: _kGold),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildBcvCard() {
    final data = _bcvData;
    return Container(
      decoration: BoxDecoration(
        color: _kCardBg,
        borderRadius: BorderRadius.circular(20),
      ),
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              const Expanded(
                child: Text(
                  'Tasas BCV',
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w500,
                    color: _kGold,
                    letterSpacing: 0.75,
                  ),
                ),
              ),
              if (data?.isCache ?? false)
                const Padding(
                  padding: EdgeInsets.only(right: 8),
                  child: Text('📦', style: TextStyle(fontSize: 10)),
                ),
              if (_bcvLoading)
                const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: _kGold,
                  ),
                )
              else
                InkWell(
                  onTap: () => _fetchBcvData(force: true),
                  child: const Padding(
                    padding: EdgeInsets.all(4),
                    child: Text('↻',
                        style: TextStyle(fontSize: 18, color: _kGold)),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              _buildRateColumn(
                'USD',
                _bcvLoading && data == null
                    ? '...'
                    : _bcvError
                        ? '---'
                        : data != null && data.dolar > 0
                            ? data.dolar.toStringAsFixed(2)
                            : 'N/A',
                error: _bcvError,
                muted: data != null && data.dolar <= 0,
              ),
              Container(width: 1, height: 60, color: _kSeparator),
              _buildRateColumn(
                'EUR',
                _bcvLoading && data == null
                    ? '...'
                    : _bcvError
                        ? '---'
                        : data != null && data.euro > 0
                            ? data.euro.toStringAsFixed(2)
                            : 'N/A',
                error: _bcvError,
                muted: data != null && data.euro <= 0,
              ),
            ],
          ),
          const SizedBox(height: 12),
          const Divider(height: 1, color: _kSeparator),
          const SizedBox(height: 8),
          Text(
            _bcvError
                ? '📅 $_bcvErrorMsg'
                : '📅 Fecha valor: ${data?.fechaValor ?? "---"}',
            style: const TextStyle(fontSize: 10, color: Color(0xFFAAAAAA)),
          ),
          const SizedBox(height: 2),
          Text(
            '🕓 Última consulta: ${data?.lastUpdate ?? "---"}',
            style: const TextStyle(fontSize: 10, color: Color(0xFFAAAAAA)),
          ),
          if (data?.isCache ?? false)
            const Text(
              '📦 Dato en caché',
              style: TextStyle(fontSize: 10, color: _kGold),
            ),
          if ((data?.vigenciaMsg ?? '').isNotEmpty)
            Text(
              data!.vigenciaMsg,
              style: const TextStyle(
                fontSize: 10,
                fontWeight: FontWeight.w500,
                color: _kGold,
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildRateColumn(String label, String value,
      {bool error = false, bool muted = false}) {
    return Expanded(
      child: Column(
        children: [
          Text(
            label,
            style: const TextStyle(fontSize: 11, color: Color(0xFFAAAAAA)),
          ),
          const SizedBox(height: 4),
          Text(
            value,
            style: TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.w500,
              color: error ? _kError : muted ? _kGoldMuted : _kText,
            ),
          ),
          const SizedBox(height: 2),
          const Text(
            'Bs.',
            style: TextStyle(fontSize: 10, color: Color(0xFFAAAAAA)),
          ),
        ],
      ),
    );
  }

  Widget _buildBinanceCard() {
    final data = _binanceData;
    return Container(
      decoration: BoxDecoration(
        color: _kCardBg,
        borderRadius: BorderRadius.circular(20),
      ),
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              const Expanded(
                child: Text(
                  'Tasa Binance',
                  style: TextStyle(
                    fontSize: 15,
                    fontWeight: FontWeight.w500,
                    color: _kGold,
                    letterSpacing: 0.75,
                  ),
                ),
              ),
              if (data?.isCache ?? false)
                const Padding(
                  padding: EdgeInsets.only(right: 8),
                  child: Text('📦', style: TextStyle(fontSize: 10)),
                ),
              if (_binanceLoading)
                const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: _kGold,
                  ),
                )
              else
                InkWell(
                  onTap: () => _fetchBinanceData(force: true),
                  child: const Padding(
                    padding: EdgeInsets.all(4),
                    child: Text('🔄', style: TextStyle(fontSize: 14)),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            _binanceLoading && data == null
                ? '...'
                : _binanceError
                    ? '---'
                    : data != null && data.price > 0
                        ? '${data.priceStr} Bs/USDT'
                        : 'N/A',
            style: TextStyle(
              fontSize: 24,
              fontWeight: FontWeight.w300,
              color: _binanceError
                  ? _kError
                  : (data != null && data.price <= 0) ? _kGoldMuted : _kText,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            _binanceError
                ? '🕓 $_binanceErrorMsg'
                : '🕓 Última consulta: ${data?.lastUpdate ?? "---"}',
            style: const TextStyle(fontSize: 10, color: Color(0xFFAAAAAA)),
          ),
          const SizedBox(height: 2),
          const Text(
            '⚠️ Valor aproximado',
            style: TextStyle(
              fontSize: 10,
              fontWeight: FontWeight.w500,
              color: _kGold,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildShareButton() {
    return Container(
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFF25D366), Color(0xFF128C7E)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: _handleShareClick,
          borderRadius: BorderRadius.circular(12),
          child: Padding(
            padding: const EdgeInsets.symmetric(vertical: 14),
            child: Text(
              _hasQr
                  ? '📤 COMPARTIR POR WHATSAPP'
                  : '📤 SELECCIONAR QR Y COMPARTIR',
              textAlign: TextAlign.center,
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.bold,
                color: Colors.white,
              ),
            ),
          ),
        ),
      ),
    );
  }

}
