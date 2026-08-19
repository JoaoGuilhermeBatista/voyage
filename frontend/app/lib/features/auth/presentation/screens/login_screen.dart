import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:supabase_flutter/supabase_flutter.dart';
import '../../../../core/themes/app_theme.dart';
import '../../../../core/utils/validators.dart';
import '../providers/auth_provider.dart';

const _mensagemConfirmacaoPendente =
    'Enviamos um email de confirmação. Verifique sua caixa de entrada antes '
    'de entrar.';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen>
    with SingleTickerProviderStateMixin {
  late final TabController _tabController;
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _obscurePassword = true;
  String? _errorMessage;
  String? _infoMessage;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
    _tabController.addListener(() => setState(() {
          _errorMessage = null;
          _infoMessage = null;
        }));
  }

  @override
  void dispose() {
    _tabController.dispose();
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() {
      _errorMessage = null;
      _infoMessage = null;
    });

    final email = _emailController.text.trim();
    final password = _passwordController.text;

    if (email.isEmpty || password.isEmpty) {
      setState(() => _errorMessage = 'Preencha todos os campos');
      return;
    }

    final emailError = validateEmail(email);
    if (emailError != null) {
      setState(() => _errorMessage = emailError);
      return;
    }

    final notifier = ref.read(authNotifierProvider.notifier);
    final isSignUp = _tabController.index != 0;

    if (!isSignUp) {
      await notifier.signIn(email, password);
    } else {
      await notifier.signUp(email, password);
    }

    final state = ref.read(authNotifierProvider);
    if (state.hasError && mounted) {
      final error = state.error;
      setState(() {
        if (error is AuthException) {
          _errorMessage = _translateError(error.message);
        } else {
          _errorMessage = 'Ocorreu um erro. Tente novamente.';
        }
      });
    } else if (isSignUp && mounted) {
      try {
        final currentSession = ref.read(supabaseProvider).auth.currentSession;
        if (currentSession == null) {
          setState(() => _infoMessage = _mensagemConfirmacaoPendente);
        }
      } catch (_) {
        // supabaseProvider não disponível/inicializado (ex.: cenário de
        // teste sem mock de SupabaseClient) — nada a reportar aqui.
      }
    }
  }

  String _translateError(String message) {
    if (message.contains('Invalid login credentials')) {
      return 'Email ou senha incorretos';
    }
    if (message.contains('Email not confirmed')) {
      return 'Confirme seu email antes de entrar';
    }
    if (message.contains('User already registered')) {
      return 'Este email já está cadastrado';
    }
    if (message.contains('Password should be')) {
      return 'A senha deve ter pelo menos 6 caracteres';
    }
    return message;
  }

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authNotifierProvider);
    final isLoading = authState.isLoading;

    return Scaffold(
      backgroundColor: AppTheme.background,
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 28),
          child: Column(
            children: [
              const SizedBox(height: 64),
              _buildHeader(),
              const SizedBox(height: 48),
              _buildTabBar(),
              const SizedBox(height: 32),
              _buildForm(isLoading),
              if (_errorMessage != null) ...[
                const SizedBox(height: 16),
                _buildError(),
              ],
              if (_infoMessage != null) ...[
                const SizedBox(height: 16),
                _buildInfo(),
              ],
              const SizedBox(height: 24),
              _buildSubmitButton(isLoading),
              const SizedBox(height: 40),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildHeader() {
    return Column(
      children: [
        Text(
          'LUVOY',
          style: Theme.of(context).textTheme.displayLarge?.copyWith(
                fontSize: 42,
                letterSpacing: 8,
                color: AppTheme.softBlack,
              ),
        ),
        const SizedBox(height: 8),
        Container(
          width: 40,
          height: 1.5,
          color: AppTheme.goldenOrange,
        ),
        const SizedBox(height: 12),
        Text(
          'Viagens de experiência',
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                color: AppTheme.softBlack.withValues(alpha:0.5),
                letterSpacing: 1.5,
              ),
        ),
      ],
    );
  }

  Widget _buildTabBar() {
    return Container(
      decoration: BoxDecoration(
        color: AppTheme.softWhite,
        borderRadius: BorderRadius.circular(14),
      ),
      padding: const EdgeInsets.all(4),
      child: TabBar(
        controller: _tabController,
        indicator: BoxDecoration(
          color: AppTheme.primaryBlue,
          borderRadius: BorderRadius.circular(10),
        ),
        indicatorSize: TabBarIndicatorSize.tab,
        labelColor: AppTheme.softWhite,
        unselectedLabelColor: AppTheme.softBlack.withValues(alpha:0.5),
        labelStyle: Theme.of(context).textTheme.bodyMedium?.copyWith(
              fontWeight: FontWeight.w600,
            ),
        dividerColor: Colors.transparent,
        tabs: const [
          Tab(text: 'Entrar'),
          Tab(text: 'Criar conta'),
        ],
      ),
    );
  }

  Widget _buildForm(bool isLoading) {
    return Column(
      children: [
        TextField(
          controller: _emailController,
          enabled: !isLoading,
          keyboardType: TextInputType.emailAddress,
          decoration: InputDecoration(
            hintText: 'Email',
            hintStyle: TextStyle(color: AppTheme.softBlack.withValues(alpha:0.35)),
            prefixIcon: Icon(
              Icons.mail_outline_rounded,
              color: AppTheme.softBlack.withValues(alpha:0.4),
              size: 20,
            ),
          ),
        ),
        const SizedBox(height: 14),
        TextField(
          controller: _passwordController,
          enabled: !isLoading,
          obscureText: _obscurePassword,
          decoration: InputDecoration(
            hintText: 'Senha',
            hintStyle: TextStyle(color: AppTheme.softBlack.withValues(alpha:0.35)),
            prefixIcon: Icon(
              Icons.lock_outline_rounded,
              color: AppTheme.softBlack.withValues(alpha:0.4),
              size: 20,
            ),
            suffixIcon: IconButton(
              icon: Icon(
                _obscurePassword
                    ? Icons.visibility_off_outlined
                    : Icons.visibility_outlined,
                color: AppTheme.softBlack.withValues(alpha:0.4),
                size: 20,
              ),
              onPressed: () =>
                  setState(() => _obscurePassword = !_obscurePassword),
            ),
          ),
          onSubmitted: (_) => _submit(),
        ),
      ],
    );
  }

  Widget _buildError() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: const Color(0xFFFFEDED),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          const Icon(Icons.error_outline_rounded,
              color: Color(0xFFB00020), size: 18),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              _errorMessage!,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: const Color(0xFFB00020),
                  ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInfo() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: AppTheme.primaryBlue.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          const Icon(Icons.mark_email_read_outlined,
              color: AppTheme.primaryBlue, size: 18),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              _infoMessage!,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    color: AppTheme.primaryBlue,
                  ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSubmitButton(bool isLoading) {
    return SizedBox(
      width: double.infinity,
      child: ElevatedButton(
        onPressed: isLoading ? null : _submit,
        child: isLoading
            ? const SizedBox(
                height: 20,
                width: 20,
                child: CircularProgressIndicator(
                  strokeWidth: 2,
                  color: AppTheme.softWhite,
                ),
              )
            : Text(
                _tabController.index == 0 ? 'Entrar' : 'Criar conta',
              ),
      ),
    );
  }
}
