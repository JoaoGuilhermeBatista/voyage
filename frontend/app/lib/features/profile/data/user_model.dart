class UserModel {
  final String id;
  final String email;
  final String? avatarUrl;
  final bool onboardingCompleted;
  final bool isPremium;
  final String? budget;
  final String? rhythm;
  final String? groupType;
  final List<String> interests;
  final String? religion;

  const UserModel({
    required this.id,
    required this.email,
    this.avatarUrl,
    required this.onboardingCompleted,
    required this.isPremium,
    this.budget,
    this.rhythm,
    this.groupType,
    this.interests = const [],
    this.religion,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id'] as String,
      email: json['email'] as String,
      avatarUrl: json['avatarUrl'] as String?,
      onboardingCompleted: json['onboardingCompleted'] as bool? ?? false,
      isPremium: json['isPremium'] as bool? ?? false,
      budget: json['budget'] as String?,
      rhythm: json['rhythm'] as String?,
      groupType: json['groupType'] as String?,
      interests: (json['interests'] as List<dynamic>?)?.cast<String>() ?? [],
      religion: json['religion'] as String?,
    );
  }
}
