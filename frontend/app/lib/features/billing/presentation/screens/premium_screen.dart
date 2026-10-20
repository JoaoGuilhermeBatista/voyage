import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:url_launcher/url_launcher.dart';
import '../../../../core/constants/deeplink_constants.dart';
import '../../../../core/themes/app_theme.dart';
import '../../../profile/data/user_model.dart';
import '../../../profile/presentation/providers/user_provider.dart';
import '../../data/billing_repository.dart';

/// Tela "Planos" — os 3 fluxos de pagamento do backend (ver
/// luvoy-pendencias.md): assinatura por cartão, premium avulso por Pix (30
/// dias, sem auto-renovação — Pix não permite cobrança recorrente no
/// Stripe) e crédito avulso de exportação de PDF por Pix (disponível pra
/// todo mundo, premium ou não — é um produto à parte).
///
/// O checkout do Stripe abre no navegador externo (nunca embutido — é a
/// recomendação do próprio Stripe, evita problema com 3D Secure e outros
/// fluxos de confirmação). O retorno pro app usa o deep link customizado
/// (ver DeeplinkService/DeeplinkConstants — ainda não há domínio hospedado
/// pra Universal Links/App Links https://). Mantemos também a revalidação
/// do perfil ao voltar de segundo plano (`AppLifecycleState.resumed`) e o
/// botão manual como reforço, já que o pagamento em si é confirmado via
/// webhook no backend, independente do deep link funcionar perfeitamente em
/// todo cenário (ex: app encerrado por completo no meio do checkout).
class PremiumScreen extends ConsumerStatefulWidget {
  const PremiumScreen({super.key});

  @override
  ConsumerState<PremiumScreen> createState() => _PremiumScreenState();
}

class _PremiumScreenState extends ConsumerState<PremiumScreen>
    with WidgetsBindingObserver {
  String? _loadingAction;

  static const _successUrl = DeeplinkConstants.checkoutSuccess;
  static const _cancelUrl = DeeplinkConstants.checkoutCancel;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    super.dispose();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      ref.invalidate(userProvider);
    }
  }

  Future<void> _startCheckout(
    String action,
    Future<String> Function() createCheckout,
  ) async {
    setState(() => _loadingAction = action);
    try {
      final checkoutUrl = await createCheckout();
      final launched = await launchUrl(
        Uri.parse(checkoutUrl),
        mode: LaunchMode.externalApplication,
      );
      if (!launched && mounted) {
        _showMessage('Não foi possível abrir a página de pagamento.');
      }
    } catch (_) {
      if (mounted) {
        _showMessage('Não foi possível iniciar o pagamento. Tente novamente.');
      }
    } finally {
      if (mounted) setState(() => _loadingAction = null);
    }
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(
      context,
    ).showSnackBar(SnackBar(content: Text(message)));
  }

  @override
  Widget build(BuildContext context) {
    final userAsync = ref.watch(userProvider);
    final repo = ref.read(billingRepositoryProvider);

    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        backgroundColor: AppTheme.background,
        elevation: 0,
        title: Text(
          'Planos',
          style: GoogleFonts.cormorantGaramond(
            fontSize: 22,
            fontWeight: FontWeight.w600,
            color: AppTheme.foreground,
          ),
        ),
      ),
      body: SafeArea(
        child: userAsync.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (_, _) =>
              const Center(child: Text('Erro ao carregar seu plano')),
          data: (user) => ListView(
            padding: const EdgeInsets.fromLTRB(24, 8, 24, 32),
            children: [
              _StatusBanner(user: user),
              const SizedBox(height: 32),
              if (!(user?.isPremium ?? false)) ...[
                _PlanOption(
                  icon: Icons.credit_card,
                  title: 'Assinatura mensal (cartão)',
                  description:
                      'Renova automaticamente todo mês. Cancele quando quiser — o acesso continua até o fim do período já pago.',
                  buttonLabel: 'Assinar com cartão',
                  loading: _loadingAction == 'card',
                  disabled: _loadingAction != null,
                  onTap: () => _startCheckout(
                    'card',
                    () => repo.createSubscriptionCheckout(
                      successUrl: _successUrl,
                      cancelUrl: _cancelUrl,
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                _PlanOption(
                  icon: Icons.qr_code,
                  title: 'Premium por Pix (30 dias)',
                  description:
                      'Pagamento único, válido por 30 dias. Não renova sozinho — pague de novo quando quiser continuar.',
                  buttonLabel: 'Pagar com Pix',
                  loading: _loadingAction == 'pix',
                  disabled: _loadingAction != null,
                  onTap: () => _startCheckout(
                    'pix',
                    () => repo.createPixPremiumCheckout(
                      successUrl: _successUrl,
                      cancelUrl: _cancelUrl,
                    ),
                  ),
                ),
                const SizedBox(height: 32),
                Divider(color: AppTheme.border),
                const SizedBox(height: 32),
              ],
              _PlanOption(
                icon: Icons.picture_as_pdf_outlined,
                title: 'Exportação avulsa de PDF',
                description:
                    'Já usou sua exportação gratuita? Compre 1 exportação extra via Pix, sem precisar assinar o Premium.',
                buttonLabel: 'Comprar via Pix',
                loading: _loadingAction == 'pdf',
                disabled: _loadingAction != null,
                onTap: () => _startCheckout(
                  'pdf',
                  () => repo.createPixPdfExportCheckout(
                    successUrl: _successUrl,
                    cancelUrl: _cancelUrl,
                  ),
                ),
              ),
              const SizedBox(height: 24),
              Center(
                child: TextButton(
                  onPressed: () => ref.invalidate(userProvider),
                  child: Text(
                    'Já paguei — atualizar status',
                    style: GoogleFonts.jost(
                      fontSize: 13,
                      fontWeight: FontWeight.w400,
                      color: AppTheme.mutedForeground,
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

// ── Status atual ─────────────────────────────────────────────────────────────

class _StatusBanner extends StatelessWidget {
  final UserModel? user;
  const _StatusBanner({required this.user});

  String _formatDate(DateTime d) =>
      '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

  @override
  Widget build(BuildContext context) {
    final isPremium = user?.isPremium ?? false;
    final expiresAt = user?.premiumExpiresAt;

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
      decoration: BoxDecoration(
        color: isPremium ? AppTheme.luxy : AppTheme.card,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(
          color: isPremium
              ? AppTheme.goldNoir.withValues(alpha: 0.4)
              : AppTheme.border,
        ),
      ),
      child: Row(
        children: [
          Icon(
            Icons.workspace_premium_outlined,
            size: 24,
            color: isPremium ? AppTheme.goldNoir : AppTheme.primary,
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  isPremium ? 'Você é Premium' : 'Plano gratuito',
                  style: GoogleFonts.jost(
                    fontSize: 15,
                    fontWeight: FontWeight.w500,
                    color: AppTheme.foreground,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  isPremium
                      ? (expiresAt != null
                            ? 'Válido até ${_formatDate(expiresAt)}'
                            : 'Acesso ilimitado')
                      : '1 roteiro e 1 exportação de PDF grátis por mês',
                  style: GoogleFonts.jost(
                    fontSize: 12.5,
                    fontWeight: FontWeight.w300,
                    color: isPremium
                        ? AppTheme.foreground.withValues(alpha: 0.7)
                        : AppTheme.mutedForeground,
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

// ── Opção de plano/compra ────────────────────────────────────────────────────

class _PlanOption extends StatelessWidget {
  final IconData icon;
  final String title;
  final String description;
  final String buttonLabel;
  final bool loading;
  final bool disabled;
  final VoidCallback onTap;

  const _PlanOption({
    required this.icon,
    required this.title,
    required this.description,
    required this.buttonLabel,
    required this.loading,
    required this.disabled,
    required this.onTap,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppTheme.card,
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: AppTheme.border),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, size: 20, color: AppTheme.primary),
              const SizedBox(width: 12),
              Expanded(
                child: Text(
                  title,
                  style: GoogleFonts.jost(
                    fontSize: 15,
                    fontWeight: FontWeight.w500,
                    color: AppTheme.foreground,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            description,
            style: GoogleFonts.jost(
              fontSize: 12.5,
              fontWeight: FontWeight.w300,
              color: AppTheme.mutedForeground,
              height: 1.4,
            ),
          ),
          const SizedBox(height: 16),
          SizedBox(
            width: double.infinity,
            child: ElevatedButton(
              onPressed: disabled ? null : onTap,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.primary,
                foregroundColor: AppTheme.primaryForeground,
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(6),
                ),
              ),
              child: loading
                  ? SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: AppTheme.primaryForeground,
                      ),
                    )
                  : Text(
                      buttonLabel,
                      style: GoogleFonts.jost(
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                        letterSpacing: 0.4,
                      ),
                    ),
            ),
          ),
        ],
      ),
    );
  }
}
