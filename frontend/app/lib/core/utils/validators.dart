String? validateEmail(String email) {
  final trimmed = email.trim();
  if (trimmed.isEmpty) return 'Email é obrigatório';
  final regex = RegExp(r'^[\w.+-]+@[\w-]+\.[\w.-]+$');
  if (!regex.hasMatch(trimmed)) return 'Digite um email válido';
  return null;
}
