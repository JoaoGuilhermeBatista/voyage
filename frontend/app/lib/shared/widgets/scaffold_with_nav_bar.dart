import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import '../../core/themes/app_theme.dart';

class ScaffoldWithNavBar extends StatelessWidget {
  final StatefulNavigationShell navigationShell;

  const ScaffoldWithNavBar({super.key, required this.navigationShell});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: navigationShell,
      bottomNavigationBar: _LuvoyNavBar(
        currentIndex: navigationShell.currentIndex,
        onTap: (index) => navigationShell.goBranch(
          index,
          initialLocation: index == navigationShell.currentIndex,
        ),
      ),
    );
  }
}

class _LuvoyNavBar extends StatelessWidget {
  final int currentIndex;
  final ValueChanged<int> onTap;

  const _LuvoyNavBar({required this.currentIndex, required this.onTap});

  static const _items = [
    _NavItemData(icon: Icons.explore_outlined, activeIcon: Icons.explore, label: 'Explorar'),
    _NavItemData(icon: Icons.auto_awesome_outlined, activeIcon: Icons.auto_awesome, label: 'Para você'),
    _NavItemData(icon: Icons.map_outlined, activeIcon: Icons.map, label: 'Roteiros'),
    _NavItemData(icon: Icons.favorite_border, activeIcon: Icons.favorite, label: 'Favoritos'),
    _NavItemData(icon: Icons.person_outline, activeIcon: Icons.person, label: 'Perfil'),
  ];

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: AppTheme.softWhite,
        border: Border(top: BorderSide(color: Color(0xFFE8E4DC))),
      ),
      child: SafeArea(
        top: false,
        child: SizedBox(
          height: 64,
          child: Row(
            children: List.generate(
              _items.length,
              (i) => _NavItem(
                data: _items[i],
                isActive: i == currentIndex,
                onTap: () => onTap(i),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _NavItemData {
  final IconData icon;
  final IconData activeIcon;
  final String label;
  const _NavItemData({required this.icon, required this.activeIcon, required this.label});
}

class _NavItem extends StatelessWidget {
  final _NavItemData data;
  final bool isActive;
  final VoidCallback onTap;

  const _NavItem({required this.data, required this.isActive, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final color = isActive ? AppTheme.goldenOrange : AppTheme.softBlack.withValues(alpha: 0.45);

    return Expanded(
      child: GestureDetector(
        behavior: HitTestBehavior.opaque,
        onTap: onTap,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(isActive ? data.activeIcon : data.icon, color: color, size: 22),
            const SizedBox(height: 3),
            Text(
              data.label,
              style: TextStyle(
                fontFamily: 'Manrope',
                fontSize: 10,
                fontWeight: isActive ? FontWeight.w600 : FontWeight.w400,
                color: color,
                letterSpacing: 0.2,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
