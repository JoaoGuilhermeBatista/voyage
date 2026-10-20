import 'dart:ui';

import 'package:flutter/material.dart';

/// Wraps [child] and progressively blurs it as it scrolls above the top of
/// the nearest [Scrollable]'s viewport, giving visual focus to whatever
/// section is entering view below it.
class ScrollFocusBlur extends StatefulWidget {
  final Widget child;
  final double maxSigma;

  const ScrollFocusBlur({super.key, required this.child, this.maxSigma = 10});

  @override
  State<ScrollFocusBlur> createState() => _ScrollFocusBlurState();
}

class _ScrollFocusBlurState extends State<ScrollFocusBlur> {
  final _contentKey = GlobalKey();
  final ValueNotifier<double> _sigma = ValueNotifier(0);
  ScrollPosition? _position;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final newPosition = Scrollable.maybeOf(context)?.position;
    if (newPosition != _position) {
      _position?.removeListener(_update);
      _position = newPosition;
      _position?.addListener(_update);
      WidgetsBinding.instance.addPostFrameCallback((_) => _update());
    }
  }

  void _update() {
    final box = _contentKey.currentContext?.findRenderObject() as RenderBox?;
    if (box == null || !box.attached) return;
    final topY = box.localToGlobal(Offset.zero).dy;
    final height = box.size.height;
    if (height <= 0) return;
    final progress = (-topY / height).clamp(0.0, 1.0);
    final next = progress * widget.maxSigma;
    if ((next - _sigma.value).abs() > 0.05) {
      _sigma.value = next;
    }
  }

  @override
  void dispose() {
    _position?.removeListener(_update);
    _sigma.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return ValueListenableBuilder<double>(
      valueListenable: _sigma,
      builder: (context, sigma, child) {
        return ImageFiltered(
          imageFilter: ImageFilter.blur(sigmaX: sigma, sigmaY: sigma),
          child: KeyedSubtree(key: _contentKey, child: child!),
        );
      },
      child: widget.child,
    );
  }
}
