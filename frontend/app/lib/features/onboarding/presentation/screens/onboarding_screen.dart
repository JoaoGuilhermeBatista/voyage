import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../../core/themes/app_theme.dart';
import '../../../profile/presentation/providers/user_provider.dart';
import '../widgets/preference_steps.dart';

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
  String? _religion;

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
      case 4:
        return _religion != null;
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
        religion: _religion,
      );

      // Marca onboarding concluído no StateProvider do router
      ref.read(onboardingCompletedProvider.notifier).state = true;

      // Recarrega o userProvider e aguarda para que o backend já esteja
      // "fresco" (onboardingCompleted = true) quando o redirect do GoRouter
      // rodar — senão ele vê o valor antigo em cache e manda de volta pro
      // onboarding mesmo com a navegação para /home já disparada.
      ref.invalidate(userProvider);
      await ref.read(userProvider.future);

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
              style: GoogleFonts.jost(),
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
                  BudgetStep(
                    selected: _budget,
                    onSelected: (v) => setState(() => _budget = v),
                  ),
                  RhythmStep(
                    selected: _rhythm,
                    onSelected: (v) => setState(() => _rhythm = v),
                  ),
                  GroupTypeStep(
                    selected: _groupType,
                    onSelected: (v) => setState(() => _groupType = v),
                  ),
                  InterestsStep(
                    selected: _interests,
                    onToggle: (v) => setState(() {
                      if (_interests.contains(v)) {
                        _interests.remove(v);
                      } else {
                        _interests.add(v);
                      }
                    }),
                  ),
                  ReligionStep(
                    selected: _religion,
                    onSelected: (v) => setState(() => _religion = v),
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
    const totalSteps = 5;
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
    final isLastStep = _currentPage == 4;
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
                  style: GoogleFonts.jost(
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
