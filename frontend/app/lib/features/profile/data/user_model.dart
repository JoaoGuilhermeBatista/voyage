class UserModel {
  final String id;
  final String email;
  final String? avatarUrl;
  final bool onboardingCompleted;
  final bool isPremium;

  const UserModel({
    required this.id,
    required this.email,
    this.avatarUrl,
    required this.onboardingCompleted,
    required this.isPremium,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id'] as String,
      email: json['email'] as String,
      avatarUrl: json['avatarUrl'] as String?,
      onboardingCompleted: json['onboardingCompleted'] as bool? ?? false,
      isPremium: json['isPremium'] as bool? ?? false,
    );
  }
}
