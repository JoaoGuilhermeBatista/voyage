import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../../core/themes/app_theme.dart';
import '../../../onboarding/presentation/widgets/preference_steps.dart';
import '../../data/user_model.dart';
import '../providers/user_provider.dart';

/// Tela de edição das preferências de viagem (orçamento, ritmo, tipo de
/// grupo e interesses) a partir do Perfil. Reaproveita os mesmos widgets de
/// seleção usados no wizard de Onboarding, mas apresentados como um único
/// formulário rolável em vez de etapas paginadas.
class EditPreferencesScreen extends ConsumerStatefulWidget {
  const EditPreferencesScreen({super.key});

  @override
  ConsumerState<EditPreferencesScreen> createState() =>
      _EditPreferencesScreenState();
}

class _EditPreferencesScreenState extends ConsumerState<EditPreferencesScreen> {
  bool _initialized = false;

  String? _initialBudget;
  String? _initialRhythm;
  String? _initialGroupType;
  Set<String> _initialInterests = const {};
  String? _initialReligion;

  String? _budget;
  String? _rhythm;
  String? _groupType;
  final Set<String> _interests = {};
  String? _religion;

  bool _isSaving = false;

  void _initializeFrom(UserModel user) {
    _initialBudget = user.budget;
    _initialRhythm = user.rhythm;
    _initialGroupType = user.groupType;
    _initialInterests = user.interests.toSet();
    _initialReligion = user.religion;

    _budget = user.budget;
    _rhythm = user.rhythm;
    _groupType = user.groupType;
    _interests
      ..clear()
      ..addAll(user.interests);
    _religion = user.religion;

    _initialized = true;
  }

  bool get _hasChanges {
    if (!_initialized) return false;
    return _budget != _initialBudget ||
        _rhythm != _initialRhythm ||
        _groupType != _initialGroupType ||
        !setEquals(_interests, _initialInterests) ||
        _religion != _initialReligion;
  }

  // Religião fica de fora daqui de propósito: diferente do onboarding (onde
  // é obrigatória), aqui ela é opcional — usuários que já completaram o
  // onboarding antes dessa pergunta existir (religion == null) não devem
  // ficar bloqueados de salvar outras mudanças de preferência.
  bool get _isFormValid =>
      _budget != null &&
      _rhythm != null &&
      _groupType != null &&
      _interests.isNotEmpty;

  Future<void> _save() async {
    if (!_isFormValid || _isSaving) return;

    setState(() => _isSaving = true);

    try {
      await ref
          .read(userRepositoryProvider)
          .updatePreferences(
            budget: _budget!,
            rhythm: _rhythm!,
            groupType: _groupType!,
            interests: _interests.toList(),
            religion: _religion,
          );

      ref.invalidate(userProvider);

      if (!mounted) return;

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Preferências atualizadas com sucesso!',
            style: GoogleFonts.jost(),
          ),
          backgroundColor: AppTheme.primaryBlue,
        ),
      );
      context.pop();
    } catch (e, stack) {
      debugPrint('[EditPreferences] Erro ao salvar preferências: $e\n$stack');
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Erro ao salvar suas preferências. Tente novamente.',
            style: GoogleFonts.jost(),
          ),
          backgroundColor: Colors.red.shade700,
        ),
      );
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final userAsync = ref.watch(userProvider);

    return Scaffold(
      backgroundColor: AppTheme.background,
      appBar: AppBar(
        backgroundColor: AppTheme.background,
        elevation: 0,
        surfaceTintColor: Colors.transparent,
        iconTheme: const IconThemeData(color: AppTheme.softBlack),
        title: Text(
          'Editar preferências',
          style: GoogleFonts.cormorantGaramond(
            fontSize: 22,
            fontWeight: FontWeight.w600,
            color: AppTheme.softBlack,
          ),
        ),
      ),
      body: SafeArea(
        child: userAsync.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (error, stackTrace) =>
              const Center(child: Text('Erro ao carregar suas preferências')),
          data: (user) {
            if (user == null) {
              return const Center(
                child: Text('Erro ao carregar suas preferências'),
              );
            }

            if (!_initialized) {
              _initializeFrom(user);
            }

            return Column(
              children: [
                Expanded(
                  child: SingleChildScrollView(
                    child: Column(
                      children: [
                        BudgetStep(
                          selected: _budget,
                          onSelected: (v) => setState(() => _budget = v),
                          showStepNumber: false,
                          scrollable: false,
                        ),
                        RhythmStep(
                          selected: _rhythm,
                          onSelected: (v) => setState(() => _rhythm = v),
                          showStepNumber: false,
                          scrollable: false,
                        ),
                        GroupTypeStep(
                          selected: _groupType,
                          onSelected: (v) => setState(() => _groupType = v),
                          showStepNumber: false,
                          scrollable: false,
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
                          showStepNumber: false,
                          scrollable: false,
                        ),
                        ReligionStep(
                          selected: _religion,
                          onSelected: (v) => setState(() => _religion = v),
                          showStepNumber: false,
                          scrollable: false,
                        ),
                      ],
                    ),
                  ),
                ),
                _buildSaveButton(),
              ],
            );
          },
        ),
      ),
    );
  }

  Widget _buildSaveButton() {
    final enabled = _hasChanges && _isFormValid && !_isSaving;
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 0, 24, 24),
      child: SizedBox(
        width: double.infinity,
        child: ElevatedButton(
          onPressed: enabled ? _save : null,
          style: ElevatedButton.styleFrom(
            backgroundColor: enabled
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
          child: _isSaving
              ? const SizedBox(
                  height: 20,
                  width: 20,
                  child: CircularProgressIndicator(
                    color: AppTheme.softWhite,
                    strokeWidth: 2,
                  ),
                )
              : Text(
                  'Salvar',
                  style: GoogleFonts.jost(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    color: enabled
                        ? AppTheme.softWhite
                        : const Color(0xFFB0A898),
                  ),
                ),
        ),
      ),
    );
  }
}
