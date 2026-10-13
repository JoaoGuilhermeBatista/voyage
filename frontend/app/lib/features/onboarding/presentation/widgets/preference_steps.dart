import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import '../../../../core/themes/app_theme.dart';

// ---------------------------------------------------------------------------
// Widgets e constantes de preferências (orçamento, ritmo, grupo, interesses)
// compartilhados entre o wizard de Onboarding e a tela de edição de
// preferências do Perfil.
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// Constantes de domínio
// ---------------------------------------------------------------------------

const budgetOptions = [
  Option(value: 'ECONOMY', label: 'Econômico', icon: Icons.savings_outlined),
  Option(
      value: 'MODERATE', label: 'Moderado', icon: Icons.account_balance_wallet_outlined),
  Option(value: 'LUXURY', label: 'Luxo', icon: Icons.diamond_outlined),
];

const rhythmOptions = [
  Option(value: 'RELAXED', label: 'Tranquilo', icon: Icons.self_improvement_outlined),
  Option(value: 'MODERATE', label: 'Equilibrado', icon: Icons.balance_outlined),
  Option(value: 'INTENSE', label: 'Intenso', icon: Icons.bolt_outlined),
];

const groupTypeOptions = [
  Option(value: 'SOLO', label: 'Solo', icon: Icons.person_outline),
  Option(value: 'COUPLE', label: 'Casal', icon: Icons.favorite_border),
  Option(value: 'GROUP', label: 'Em grupo', icon: Icons.group_outlined),
  Option(value: 'FAMILY', label: 'Família', icon: Icons.diversity_3_outlined),
];

const interestOptions = [
  'cultura',
  'gastronomia',
  'natureza',
  'moda',
  'bem-estar',
  'vida-noturna',
  'vida-local',
  'musica',
  'vinhos',
  'neve',
];

const interestLabels = {
  'cultura': 'Cultura',
  'gastronomia': 'Gastronomia',
  'natureza': 'Natureza',
  'moda': 'Moda',
  'bem-estar': 'Bem-estar',
  'vida-noturna': 'Vida Noturna',
  'vida-local': 'Vida Local',
  'musica': 'Música & Shows',
  'vinhos': 'Vinhos',
  'neve': 'Neve',
};

const interestIcons = {
  'cultura': Icons.museum_outlined,
  'gastronomia': Icons.restaurant_outlined,
  'natureza': Icons.park_outlined,
  'moda': Icons.checkroom_outlined,
  'bem-estar': Icons.spa_outlined,
  'vida-noturna': Icons.local_bar_outlined,
  'vida-local': Icons.storefront_outlined,
  'musica': Icons.music_note_outlined,
  'vinhos': Icons.wine_bar_outlined,
  'neve': Icons.ac_unit_outlined,
};

const religionOptions = [
  Option(value: 'CATHOLIC', label: 'Católica', icon: Icons.church_outlined),
  Option(
      value: 'EVANGELICAL', label: 'Evangélica', icon: Icons.menu_book_outlined),
  Option(
      value: 'OTHER_OR_NONE',
      label: 'Outra ou sem religião',
      icon: Icons.public_outlined),
  Option(
      value: 'PREFER_NOT_TO_SAY',
      label: 'Prefiro não informar',
      icon: Icons.visibility_off_outlined),
];

// ---------------------------------------------------------------------------
// Modelo auxiliar imutável para as opções
// ---------------------------------------------------------------------------

class Option {
  final String value;
  final String label;
  final IconData icon;

  const Option({
    required this.value,
    required this.label,
    required this.icon,
  });
}

// ---------------------------------------------------------------------------
// Etapa — Orçamento
// ---------------------------------------------------------------------------

class BudgetStep extends StatelessWidget {
  final String? selected;
  final ValueChanged<String> onSelected;
  final bool showStepNumber;
  final bool scrollable;

  const BudgetStep({
    super.key,
    required this.selected,
    required this.onSelected,
    this.showStepNumber = true,
    this.scrollable = true,
  });

  @override
  Widget build(BuildContext context) {
    return StepScaffold(
      stepNumber: '01',
      showStepNumber: showStepNumber,
      scrollable: scrollable,
      title: 'Qual é o seu\norçamento de viagem?',
      subtitle: 'Nos ajude a personalizar os melhores destinos para você.',
      child: Column(
        children: budgetOptions
            .map((opt) => SelectionCard(
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
// Etapa — Ritmo de viagem
// ---------------------------------------------------------------------------

class RhythmStep extends StatelessWidget {
  final String? selected;
  final ValueChanged<String> onSelected;
  final bool showStepNumber;
  final bool scrollable;

  const RhythmStep({
    super.key,
    required this.selected,
    required this.onSelected,
    this.showStepNumber = true,
    this.scrollable = true,
  });

  @override
  Widget build(BuildContext context) {
    return StepScaffold(
      stepNumber: '02',
      showStepNumber: showStepNumber,
      scrollable: scrollable,
      title: 'Como você prefere\nexplorar um destino?',
      subtitle: 'Defina o ritmo ideal para as suas aventuras.',
      child: Column(
        children: rhythmOptions
            .map((opt) => SelectionCard(
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
// Etapa — Tipo de grupo
// ---------------------------------------------------------------------------

class GroupTypeStep extends StatelessWidget {
  final String? selected;
  final ValueChanged<String> onSelected;
  final bool showStepNumber;
  final bool scrollable;

  const GroupTypeStep({
    super.key,
    required this.selected,
    required this.onSelected,
    this.showStepNumber = true,
    this.scrollable = true,
  });

  @override
  Widget build(BuildContext context) {
    return StepScaffold(
      stepNumber: '03',
      showStepNumber: showStepNumber,
      scrollable: scrollable,
      title: 'Com quem você\ncostuma viajar?',
      subtitle: 'Vamos sugerir experiências para o seu estilo de grupo.',
      child: GridView.count(
        crossAxisCount: 2,
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        mainAxisSpacing: 12,
        crossAxisSpacing: 12,
        childAspectRatio: 1.3,
        children: groupTypeOptions
            .map((opt) => SelectionCard(
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
// Etapa — Religião
// ---------------------------------------------------------------------------

class ReligionStep extends StatelessWidget {
  final String? selected;
  final ValueChanged<String> onSelected;
  final bool showStepNumber;
  final bool scrollable;

  const ReligionStep({
    super.key,
    required this.selected,
    required this.onSelected,
    this.showStepNumber = true,
    this.scrollable = true,
  });

  @override
  Widget build(BuildContext context) {
    return StepScaffold(
      stepNumber: '05',
      showStepNumber: showStepNumber,
      scrollable: scrollable,
      title: 'Qual a sua\nreligião?',
      subtitle:
          'Isso nos ajuda a personalizar sugestões de locais e experiências religiosas.',
      child: Column(
        children: religionOptions
            .map((opt) => SelectionCard(
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
// Etapa — Interesses
// ---------------------------------------------------------------------------

class InterestsStep extends StatelessWidget {
  final Set<String> selected;
  final ValueChanged<String> onToggle;
  final bool showStepNumber;
  final bool scrollable;

  const InterestsStep({
    super.key,
    required this.selected,
    required this.onToggle,
    this.showStepNumber = true,
    this.scrollable = true,
  });

  @override
  Widget build(BuildContext context) {
    return StepScaffold(
      stepNumber: '04',
      showStepNumber: showStepNumber,
      scrollable: scrollable,
      title: 'O que mais te\nencanta em viagens?',
      subtitle: 'Escolha quantos quiser. Podemos personalizar ainda mais.',
      child: Wrap(
        spacing: 10,
        runSpacing: 10,
        children: interestOptions.map((interest) {
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
                    interestIcons[interest] ?? Icons.star_outline,
                    size: 16,
                    color: isSelected ? AppTheme.goldenOrange : AppTheme.softBlack,
                  ),
                  const SizedBox(width: 6),
                  Text(
                    interestLabels[interest] ?? interest,
                    style: GoogleFonts.jost(
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

class StepScaffold extends StatelessWidget {
  final String stepNumber;
  final String title;
  final String subtitle;
  final Widget child;
  final bool showStepNumber;

  /// Quando true (padrão, usado pelo wizard de Onboarding dentro de um
  /// PageView), a seção se torna rolável por conta própria — necessário
  /// porque cada página do PageView recebe altura fixa. Quando false (usado
  /// pela tela de edição de preferências, onde várias seções são empilhadas
  /// dentro de um único SingleChildScrollView externo), a seção apenas ocupa
  /// sua altura natural, evitando aninhar scrolls verticais um dentro do
  /// outro.
  final bool scrollable;

  const StepScaffold({
    super.key,
    required this.stepNumber,
    required this.title,
    required this.subtitle,
    required this.child,
    this.showStepNumber = true,
    this.scrollable = true,
  });

  @override
  Widget build(BuildContext context) {
    final content = Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        const SizedBox(height: 8),
        if (showStepNumber) ...[
          Text(
            stepNumber,
            style: GoogleFonts.jost(
              fontSize: 12,
              fontWeight: FontWeight.w600,
              letterSpacing: 2,
              color: AppTheme.goldenOrange,
            ),
          ),
          const SizedBox(height: 10),
        ],
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
          style: GoogleFonts.jost(
            fontSize: 14,
            color: const Color(0xFF666666),
            height: 1.5,
          ),
        ),
        const SizedBox(height: 28),
        child,
        const SizedBox(height: 24),
      ],
    );

    if (!scrollable) {
      return Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24),
        child: content,
      );
    }

    return SingleChildScrollView(
      padding: const EdgeInsets.symmetric(horizontal: 24),
      child: content,
    );
  }
}

// ---------------------------------------------------------------------------
// Card de seleção única (orçamento, ritmo, grupo)
// ---------------------------------------------------------------------------

class SelectionCard extends StatelessWidget {
  final Option option;
  final bool isSelected;
  final VoidCallback onTap;
  final bool isCompact;

  const SelectionCard({
    super.key,
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
                    size: 34,
                    color: isSelected
                        ? AppTheme.goldenOrange
                        : AppTheme.primaryBlue,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    option.label,
                    textAlign: TextAlign.center,
                    style: GoogleFonts.jost(
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
                    style: GoogleFonts.jost(
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
