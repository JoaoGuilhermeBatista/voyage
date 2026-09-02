import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import '../../../../core/themes/app_theme.dart';
import '../providers/user_provider.dart';

/// Tela "Perfil" — spec LUVOY §8. Card "Meu Plano" reflete o campo real
/// `user.isPremium`: variante Free em tema claro, variante Premium como
/// bloco Noir de verdade (fundo `ink`, dourado `goldNoir`), igual ao bloco
/// Noite do roteiro. O CTA "Tornar-se Premium" do spec original foi
/// deliberadamente omitido — não existe fluxo de assinatura/checkout real
/// no app hoje, então o card fica informativo (mesmo critério usado para
/// não construir Concierge/Checklists/paywall, ver luvoy-pendencias.md).
class ProfileScreen extends ConsumerWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final userAsync = ref.watch(userProvider);

    return Scaffold(
      backgroundColor: AppTheme.background,
      body: SafeArea(
        child: userAsync.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (_, _) => const Center(child: Text('Erro ao carregar perfil')),
          data: (user) => ListView(
            padding: const EdgeInsets.symmetric(horizontal: 24),
            children: [
              const SizedBox(height: 48),
              _ProfileHeader(email: user?.email ?? ''),
              const SizedBox(height: 40),
              _ActionCard(
                icon: Icons.tune_outlined,
                title: 'Minhas Preferências',
                subtitle: 'Estilos de viagem, gastronomia e clima',
                onTap: () => context.push('/perfil/preferencias'),
              ),
              const SizedBox(height: 12),
              _PlanCard(isPremium: user?.isPremium ?? false),
              const SizedBox(height: 12),
              _ActionCard(
                icon: Icons.info_outline,
                title: 'Sobre o LUVOY',
                subtitle: 'Curadoria premium de experiências de viagem',
                onTap: () => _showAbout(context),
              ),
              const SizedBox(height: 48),
              _LogoutButton(ref: ref),
              const SizedBox(height: 32),
            ],
          ),
        ),
      ),
    );
  }

  void _showAbout(BuildContext context) {
    showDialog(
      context: context,
      builder: (dialogContext) => AlertDialog(
        backgroundColor: AppTheme.card,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        title: Text(
          'LUVOY',
          style: GoogleFonts.cormorantGaramond(
            fontSize: 24,
            fontWeight: FontWeight.w600,
            color: AppTheme.foreground,
          ),
        ),
        content: Text(
          'Curadoria premium de experiências e roteiros de viagem europeia.',
          style: GoogleFonts.jost(
            fontSize: 14,
            fontWeight: FontWeight.w300,
            color: AppTheme.mutedForeground,
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogContext).pop(),
            child: Text(
              'Fechar',
              style: GoogleFonts.jost(
                color: AppTheme.primary,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// ── Header ─────────────────────────────────────────────────────────────────

class _ProfileHeader extends StatelessWidget {
  final String email;

  const _ProfileHeader({required this.email});

  String get _initials {
    final prefix = email.split('@').first;
    if (prefix.isEmpty) return '?';
    final parts = prefix.split('.');
    if (parts.length >= 2) {
      return '${parts[0][0]}${parts[1][0]}'.toUpperCase();
    }
    return prefix[0].toUpperCase();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Container(
          width: 96,
          height: 96,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            color: AppTheme.secondary,
            border: Border.all(color: AppTheme.primary, width: 1.5),
          ),
          child: Center(
            child: Text(
              _initials,
              style: GoogleFonts.cormorantGaramond(
                fontSize: 32,
                fontWeight: FontWeight.w500,
                color: AppTheme.primary,
              ),
            ),
          ),
        ),
        const SizedBox(height: 16),
        Text(
          email
              .split('@')
              .first
              .replaceAll('.', ' ')
              .split(' ')
              .map((w) {
                if (w.isEmpty) return w;
                return w[0].toUpperCase() + w.substring(1);
              })
              .join(' '),
          style: GoogleFonts.cormorantGaramond(
            fontSize: 28,
            fontWeight: FontWeight.w500,
            color: AppTheme.foreground,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          email,
          style: GoogleFonts.jost(
            fontSize: 13,
            fontWeight: FontWeight.w300,
            color: AppTheme.mutedForeground,
          ),
        ),
      ],
    );
  }
}

// ── Action card (Minhas Preferências / Sobre o LUVOY) ───────────────────────

class _ActionCard extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  const _ActionCard({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppTheme.card,
      borderRadius: BorderRadius.circular(8),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: AppTheme.border),
          ),
          child: Row(
            children: [
              Icon(icon, size: 20, color: AppTheme.mutedForeground),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: GoogleFonts.jost(
                        fontSize: 15,
                        fontWeight: FontWeight.w500,
                        color: AppTheme.foreground,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      subtitle,
                      style: GoogleFonts.jost(
                        fontSize: 12,
                        fontWeight: FontWeight.w300,
                        color: AppTheme.mutedForeground,
                      ),
                    ),
                  ],
                ),
              ),
              Icon(
                Icons.chevron_right,
                size: 20,
                color: AppTheme.mutedForeground.withValues(alpha: 0.6),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ── Card "Meu Plano" ─────────────────────────────────────────────────────────

class _PlanCard extends StatelessWidget {
  final bool isPremium;
  const _PlanCard({required this.isPremium});

  @override
  Widget build(BuildContext context) {
    if (isPremium) {
      return Container(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        decoration: BoxDecoration(
          color: AppTheme.ink,
          borderRadius: BorderRadius.circular(8),
          border: Border.all(color: AppTheme.goldNoir.withValues(alpha: 0.4)),
        ),
        child: Row(
          children: [
            const Icon(
              Icons.workspace_premium_outlined,
              size: 22,
              color: AppTheme.goldNoir,
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _Badge(
                    label: 'NOIR',
                    background: AppTheme.goldNoir,
                    foreground: AppTheme.ink,
                  ),
                  const SizedBox(height: 6),
                  Text(
                    'Acesso completo à curadoria Noir',
                    style: GoogleFonts.jost(
                      fontSize: 12,
                      fontWeight: FontWeight.w300,
                      color: AppTheme.ivory.withValues(alpha: 0.7),
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      );
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
      decoration: BoxDecoration(
        color: AppTheme.card,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppTheme.border),
      ),
      child: Row(
        children: [
          Icon(
            Icons.workspace_premium_outlined,
            size: 22,
            color: AppTheme.primary,
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _Badge(
                  label: 'GRATUITO',
                  background: AppTheme.goldSoft,
                  foreground: AppTheme.foreground,
                  border: AppTheme.primary,
                ),
                const SizedBox(height: 6),
                Text(
                  'Acesso às coleções essenciais',
                  style: GoogleFonts.jost(
                    fontSize: 12,
                    fontWeight: FontWeight.w300,
                    color: AppTheme.mutedForeground,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _Badge extends StatelessWidget {
  final String label;
  final Color background;
  final Color foreground;
  final Color? border;

  const _Badge({
    required this.label,
    required this.background,
    required this.foreground,
    this.border,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
      decoration: BoxDecoration(
        color: background,
        borderRadius: BorderRadius.circular(999),
        border: border != null ? Border.all(color: border!) : null,
      ),
      child: Text(
        label,
        style: GoogleFonts.jost(
          fontSize: 10,
          fontWeight: FontWeight.w600,
          letterSpacing: 0.8,
          color: foreground,
        ),
      ),
    );
  }
}

// ── Logout ───────────────────────────────────────────────────────────────────

class _LogoutButton extends StatefulWidget {
  final WidgetRef ref;
  const _LogoutButton({required this.ref});

  @override
  State<_LogoutButton> createState() => _LogoutButtonState();
}

class _LogoutButtonState extends State<_LogoutButton> {
  bool _loading = false;

  Future<void> _logout() async {
    setState(() => _loading = true);
    try {
      await Supabase.instance.client.auth.signOut();
      widget.ref.invalidate(userProvider);
      widget.ref.read(onboardingCompletedProvider.notifier).state = false;
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Erro ao sair. Tente novamente.')),
        );
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Center(
      child: TextButton.icon(
        onPressed: _loading ? null : _logout,
        icon: _loading
            ? SizedBox(
                width: 12,
                height: 12,
                child: CircularProgressIndicator(
                  strokeWidth: 1.5,
                  color: AppTheme.mutedForeground,
                ),
              )
            : const Icon(Icons.logout, size: 14),
        label: Text(
          'Sair',
          style: GoogleFonts.jost(
            fontSize: 11,
            fontWeight: FontWeight.w500,
            letterSpacing: 2.2,
          ),
        ),
        style: TextButton.styleFrom(foregroundColor: AppTheme.mutedForeground),
      ),
    );
  }
}
