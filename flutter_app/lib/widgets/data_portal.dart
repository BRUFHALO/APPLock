/// Fondo animado de partículas conectadas.
///
/// Port de DataPortalView.java: 35 partículas que rebotan en los bordes y se
/// unen con líneas cuando están a menos de 350 px de distancia.
library;

import 'dart:math';

import 'package:flutter/material.dart';

const int _kParticleCount = 35;
const double _kConnectionDistance = 110;
const double _kParticleSpeed = 0.15;

class DataPortal extends StatefulWidget {
  const DataPortal({super.key});

  @override
  State<DataPortal> createState() => _DataPortalState();
}

class _DataPortalState extends State<DataPortal>
    with SingleTickerProviderStateMixin {
  late final AnimationController _controller;
  final Random _random = Random();
  List<_Particle> _particles = [];

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 1),
    )..repeat();
  }

  void _initParticles(Size size) {
    _particles = List.generate(
      _kParticleCount,
      (_) => _Particle(
        x: _random.nextDouble() * size.width,
        y: _random.nextDouble() * size.height,
        vx: (_random.nextDouble() - 0.5) * _kParticleSpeed,
        vy: (_random.nextDouble() - 0.5) * _kParticleSpeed,
        radius: 2 + _random.nextDouble() * 2.5,
      ),
    );
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final size = Size(constraints.maxWidth, constraints.maxHeight);
        if (_particles.isEmpty && size.width > 0 && size.height > 0) {
          _initParticles(size);
        }
        return AnimatedBuilder(
          animation: _controller,
          builder: (context, _) => CustomPaint(
            size: size,
            painter: _DataPortalPainter(_particles),
          ),
        );
      },
    );
  }
}

class _DataPortalPainter extends CustomPainter {
  _DataPortalPainter(this.particles);

  final List<_Particle> particles;

  static final Paint _particlePaint = Paint()
    ..color = const Color(0x59E6C163)
    ..style = PaintingStyle.fill;

  static final Paint _linePaint = Paint()
    ..color = const Color(0x22E6C163)
    ..strokeWidth = 1.8
    ..style = PaintingStyle.stroke;

  @override
  void paint(Canvas canvas, Size size) {
    for (final p in particles) {
      p.update(size.width, size.height);
      canvas.drawCircle(Offset(p.x, p.y), p.radius, _particlePaint);
    }

    for (int i = 0; i < particles.length; i++) {
      for (int j = i + 1; j < particles.length; j++) {
        final p1 = particles[i];
        final p2 = particles[j];
        final dx = p1.x - p2.x;
        final dy = p1.y - p2.y;
        if (dx * dx + dy * dy < _kConnectionDistance * _kConnectionDistance) {
          canvas.drawLine(Offset(p1.x, p1.y), Offset(p2.x, p2.y), _linePaint);
        }
      }
    }
  }

  @override
  bool shouldRepaint(_DataPortalPainter oldDelegate) => true;
}

class _Particle {
  _Particle({
    required this.x,
    required this.y,
    required this.vx,
    required this.vy,
    required this.radius,
  });

  double x;
  double y;
  double vx;
  double vy;
  final double radius;

  void update(double width, double height) {
    x += vx;
    y += vy;

    if (x < 0 || x > width) vx = -vx;
    if (y < 0 || y > height) vy = -vy;

    x = x.clamp(0, width);
    y = y.clamp(0, height);
  }
}
