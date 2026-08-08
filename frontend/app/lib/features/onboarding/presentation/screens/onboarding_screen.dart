import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../../core/themes/app_theme.dart';
import '../../../profile/presentation/providers/user_provider.dart';

// ---------------------------------------------------------------------------
// Constantes de domínio
// ---------------------------------------------------------------------------

const _budgetOptions = [
  _Option(value: 'ECONOMY', label: 'Econômico', icon: Icons.savings_outlined),
  _Option(
      value: 'MODERATE', label: 'Moderado', icon: Icons.account_balance_wallet_outlined),
  _Option(value: 'LUXURY', label: 'Luxo', icon: Icons.diamond_outlined),
];

const _rhythmOptions = [
  _Option(value: 'SLOW', label: 'Tranquilo', icon: Icons.self_improvement_outlined),
  _Option(value: 'BALANCED', label: 'Equilibrado', icon: Icons.balance_outlined),
  _Option(value: 'INTENSE', label: 'Intenso', icon: Icons.bolt_outlined),
];

const _groupTypeOptions = [
  _Option(value: 'SOLO', label: 'Solo', icon: Icons.person_outline),
  _Option(value: 'COUPLE', label: 'Casal', icon: Icons.favorite_border),
  _Option(value: 'FRIENDS', label: 'Amigos', icon: Icons.group_outlined),
  _Option(value: 'FAMILY', label: 'Família', icon: Icons.family_restroom_outlined),
];

const _interestOptions = [
  'cultura',
  'gastronomia',
  'praia',
  'arte',
  'história',
  'arquitetura',
  'natureza',
  'moda',
];

const _interestLabels = {
  'cultura': 'Cultura',
  'gastronomia': 'Gastronomia',
  'praia': 'Praia',
  'arte': 'Arte',
  'história': 'História',
  'arquitetura': 'Arquitetura',
  'natureza': 'Natureza',
  'moda': 'Moda',
};

const _interestIcons = {
  'cultura': Icons.museum_outlined,
  'gastronomia': Icons.restaurant_outlined,
  'praia': Icons.beach_access_outlined,
  'arte': Icons.palette_outlined,
  'história': Icons.account_balance_outlined,
  'arquitetura': Icons.apartment_outlined,
  'natureza': Icons.park_outlined,
  'moda': Icons.checkroom_outlined,
};

// ---------------------------------------------------------------------------
// Modelo auxiliar imutável para as opções
// ---------------------------------------------------------------------------

class _Option {
  final String value;
  final String label;
  final IconData icon;

  const _Option({
    required this.value,
    required this.label,
    required this.icon,
  });
}

// ---------------------------------------------------------------------------
// Tela de Onboarding
// ---------------------------------------------------------------------------

class OnboardingScreen extends ConsumerStatefulWidget {
  const OnboardingScreen({super.key});

  @override
  ConsumerState<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends ConsumerState<OnboardingScreen> {
  final PageController _pageController = PageController();
  int _currentPage = 0;

  // Seleções do usuário
  String? _budget;
  String? _rhythm;
  String? _groupType;
  final Set<String> _interests = {};

  bool _isLoading = false;

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  // Avança para a próxima etapa
  void _nextPage() {
    _pageController.nextPage(
      duration: const Duration(milliseconds: 350),
      curve: Curves.easeInOut,
    );
  }

  // Verifica se a etapa atual está completa para habilitar o botão
  bool get _currentStepValid {
    switch (_currentPage) {
      case 0:
        return _budget != null;
      case 1:
        return _rhythm != null;
      case 2:
        return _groupType != null;
      case 3:
        return _interests.isNotEmpty;
      default:
        return false;
    }
  }

  Future<void> _submit() async {
    if (!_currentStepValid) return;

    setState(() => _isLoading = true);

    try {
      final repo = ref.read(userRepositoryProvider);
      // Garante que o user existe no banco antes de salvar preferências
      await repo.fetchMe();
      await repo.updatePreferences(
        budget: _budget!,
        rhythm: _rhythm!,
        groupType: _groupType!,
        interests: _interests.toList(),
      );

      // Marca onboarding concluído no StateProvider do router
      ref.read(onboardingCompletedProvider.notifier).state = true;

      if (mounted) {
        context.go('/home');
      }
    } catch (e, stack) {
      debugPrint('[Onboarding] Erro ao salvar preferências: $e\n$stack');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'Erro ao salvar suas preferências. Tente novamente.',
              style: GoogleFonts.manrope(),
            ),
            backgroundColor: Colors.red.shade700,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.background,
      body: SafeArea(
        child: Column(
          children: [
            _buildProgressBar(),
            Expanded(
              child: PageView(
                controller: _pageController,
                physics: const NeverScrollableScrollPhysics(),
                onPageChanged: (index) => setState(() => _currentPage = index),
                children: [
                  _BudgetStep(
                    selected: _budget,
                    onSelected: (v) => setState(() => _budget = v),
                  ),
                  _RhythmStep(
                    selected: _rhythm,
                    onSelected: (v) => setState(() => _rhythm = v),
                  ),
                  _GroupTypeStep(
                    selected: _groupType,
                    onSelected: (v) => setState(() => _groupType = v),
                  ),
                  _InterestsStep(
                    selected: _interests,
                    onToggle: (v) => setState(() {
                      if (_interests.contains(v)) {
                        _interests.remove(v);
                      } else {
                        _interests.add(v);
                      }
                    }),
                  ),
                ],
              ),
            ),
            _buildBottomButton(),
          ],
        ),
      ),
    );
  }

  Widget _buildProgressBar() {
    const totalSteps = 4;
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 20),
      child: Row(
        children: List.generate(totalSteps, (i) {
          final isActive = i <= _currentPage;
          return Expanded(
            child: Container(
              margin: EdgeInsets.only(right: i < totalSteps - 1 ? 6 : 0),
              height: 4,
              decoration: BoxDecoration(
                color: isActive
                    ? AppTheme.goldenOrange
                    : AppTheme.shimmerBase,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
          );
        }),
      ),
    );
  }

  Widget _buildBottomButton() {
    final isLastStep = _currentPage == 3;
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 0, 24, 32),
      child: SizedBox(
        width: double.infinity,
        child: ElevatedButton(
          onPressed: _currentStepValid
              ? (isLastStep ? _submit : _nextPage)
              : null,
          style: ElevatedButton.styleFrom(
            backgroundColor: _currentStepValid
                ? AppTheme.primaryBlue
                : AppTheme.shimmerBase,
            disabledBackgroundColor: AppTheme.shimmerBase,
            foregroundColor: AppTheme.softWhite,
            disabledForegroundColor: AppTheme.shimmerBase,
            padding: const EdgeInsets.symmetric(vertical: 16),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(14),
            ),
            elevation: 0,
          ),
          child: _isLoading
              ? const SizedBox(
                  height: 20,
                  width: 20,
                  child: CircularProgressIndicator(
                    color: AppTheme.softWhite,
                    strokeWidth: 2,
                  ),
                )
              : Text(
                  isLastStep ? 'Começar minha jornada' : 'Continuar',
                  style: GoogleFonts.manrope(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    color: _currentStepValid
                        ? AppTheme.softWhite
                        : const Color(0xFFB0A898),
                  ),
                ),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Etapa 1 — Orçamento
// ---------------------------------------------------------------------------

class _BudgetStep extends StatelessWidget {
  final String? selected;
  final ValueChanged<String> onSelected;

  const _BudgetStep({required this.selected, required this.onSelected});

  @override
  Widget build(BuildContext context) {
    return _StepScaffold(
      stepNumber: '01',
      title: 'Qual é o seu\norçamento de viagem?',
      subtitle: 'Nos ajude a personalizar os melhores destinos para você.',
      child: Column(
        children: _budgetOptions
            .map((opt) => _SelectionCard(
                  option: opt,
                  isSelected: selected == opt.value,
                  onTap: () => onSelected(opt.value),
                ))
            .toList(),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Etapa 2 — Ritmo de viagem
// ---------------------------------------------------------------------------

class _RhythmStep extends StatelessWidget {
  final String? selected;
  final ValueChanged<String> onSelected;

  const _RhythmStep({required this.selected, required this.onSelected});

  @override
  Widget build(BuildContext context) {
    return _StepScaffold(
      stepNumber: '02',
      title: 'Como você prefere\nexplorar um destino?',
      subtitle: 'Defina o ritmo ideal para as suas aventuras.',
      child: Column(
        children: _rhythmOptions
            .map((opt) => _SelectionCard(
                  option: opt,
                  isSelected: selected == opt.value,
                  onTap: () => onSelected(opt.value),
                ))
            .toList(),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Etapa 3 — Tipo de grupo
// ---------------------------------------------------------------------------

class _GroupTypeStep extends StatelessWidget {
  final String? selected;
  final ValueChanged<String> onSelected;

  const _GroupTypeStep({required this.selected, required this.onSelected});

  @override
  Widget build(BuildContext context) {
    return _StepScaffold(
      stepNumber: '03',
      title: 'Com quem você\ncostuma viajar?',
      subtitle: 'Vamos sugerir experiências para o seu estilo de grupo.',
      child: GridView.count(
        crossAxisCount: 2,
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        mainAxisSpacing: 12,
        crossAxisSpacing: 12,
        childAspectRatio: 1.3,
        children: _groupTypeOptions
            .map((opt) => _SelectionCard(
                  option: opt,
                  isSelected: selected == opt.value,
                  onTap: () => onSelected(opt.value),
                  isCompact: true,
                ))
            .toList(),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Etapa 4 — Interesses
// ---------------------------------------------------------------------------

class _InterestsStep extends StatelessWidget {
  final Set<String> selected;
  final ValueChanged<String> onToggle;

  const _InterestsStep({required this.selected, required this.onToggle});

  @override
  Widget build(BuildContext context) {
    return _StepScaffold(
      stepNumber: '04',
      title: 'O que mais te\nencanta em viagens?',
      subtitle: 'Escolha quantos quiser. Podemos personalizar ainda mais.',
      child: Wrap(
        spacing: 10,
        runSpacing: 10,
        children: _interestOptions.map((interest) {
          final isSelected = selected.contains(interest);
          return GestureDetector(
            onTap: () => onToggle(interest),
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 200),
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
              decoration: BoxDecoration(
                color: isSelected ? AppTheme.goldenOrange.withAlpha(26) : AppTheme.softWhite,
                borderRadius: BorderRadius.circular(32),
                border: Border.all(
                  color: isSelected ? AppTheme.goldenOrange : const Color(0xFFE8E4DC),
                  width: isSelected ? 2 : 1,
                ),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    _interestIcons[interest] ?? Icons.star_outline,
                    size: 16,
                    color: isSelected ? AppTheme.goldenOrange : AppTheme.softBlack,
                  ),
                  const SizedBox(width: 6),
                  Text(
                    _interestLabels[interest] ?? interest,
                    style: GoogleFonts.manrope(
                      fontSize: 13,
                      fontWeight: isSelected ? FontWeight.w600 : FontWeight.w500,
                      color: isSelected ? AppTheme.goldenOrange : AppTheme.softBlack,
                    ),
                  ),
                ],
              ),
            ),
          );
        }).toList(),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Layout base de cada etapa
// ---------------------------------------------------------------------------

class _StepScaffold extends StatelessWidget {
  final String stepNumber;
  final String title;
  final String subtitle;
  final Widget child;

  const _StepScaffold({
    required this.stepNumber,
    required this.title,
    required this.subtitle,
    required this.child,
  });

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const SizedBox(height: 8),
          Text(
            stepNumber,
            style: GoogleFonts.manrope(
              fontSize: 12,
              fontWeight: FontWeight.w600,
              letterSpacing: 2,
              color: AppTheme.goldenOrange,
            ),
          ),
          const SizedBox(height: 10),
          Text(
            title,
            style: GoogleFonts.cormorantGaramond(
              fontSize: 32,
              fontWeight: FontWeight.w600,
              color: AppTheme.softBlack,
              height: 1.15,
            ),
          ),
          const SizedBox(height: 8),
          Text(
            subtitle,
            style: GoogleFonts.manrope(
              fontSize: 14,
              color: const Color(0xFF666666),
              height: 1.5,
            ),
          ),
          const SizedBox(height: 28),
          child,
          const SizedBox(height: 24),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Card de seleção única (orçamento, ritmo, grupo)
// ---------------------------------------------------------------------------

class _SelectionCard extends StatelessWidget {
  final _Option option;
  final bool isSelected;
  final VoidCallback onTap;
  final bool isCompact;

  const _SelectionCard({
    required this.option,
    required this.isSelected,
    required this.onTap,
    this.isCompact = false,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onTap,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 200),
        width: double.infinity,
        margin: isCompact
            ? EdgeInsets.zero
            : const EdgeInsets.only(bottom: 12),
        padding: EdgeInsets.symmetric(
          horizontal: isCompact ? 12 : 20,
          vertical: isCompact ? 12 : 16,
        ),
        decoration: BoxDecoration(
          color: isSelected
              ? AppTheme.goldenOrange.withAlpha(26)
              : AppTheme.softWhite,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: isSelected ? AppTheme.goldenOrange : const Color(0xFFE8E4DC),
            width: isSelected ? 2 : 1,
          ),
        ),
        child: isCompact
            ? Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(
                    option.icon,
                    size: 28,
                    color: isSelected
                        ? AppTheme.goldenOrange
                        : AppTheme.primaryBlue,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    option.label,
                    textAlign: TextAlign.center,
                    style: GoogleFonts.manrope(
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                      color: isSelected
                          ? AppTheme.goldenOrange
                          : AppTheme.softBlack,
                    ),
                  ),
                ],
              )
            : Row(
                children: [
                  Container(
                    width: 44,
                    height: 44,
                    decoration: BoxDecoration(
                      color: isSelected
                          ? AppTheme.goldenOrange.withAlpha(51)
                          : AppTheme.background,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Icon(
                      option.icon,
                      size: 22,
                      color: isSelected
                          ? AppTheme.goldenOrange
                          : AppTheme.primaryBlue,
                    ),
                  ),
                  const SizedBox(width: 16),
                  Text(
                    option.label,
                    style: GoogleFonts.manrope(
                      fontSize: 15,
                      fontWeight: FontWeight.w600,
                      color: isSelected
                          ? AppTheme.goldenOrange
                          : AppTheme.softBlack,
                    ),
                  ),
                  const Spacer(),
                  if (isSelected)
                    const Icon(
                      Icons.check_circle_rounded,
                      color: AppTheme.goldenOrange,
                      size: 22,
                    ),
                ],
              ),
      ),
    );
  }
}
