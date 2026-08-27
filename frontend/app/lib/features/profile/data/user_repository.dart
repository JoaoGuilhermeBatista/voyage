import '../../../core/services/api_client.dart';
import 'user_model.dart';

class UserRepository {
  final ApiClient _apiClient;

  UserRepository(this._apiClient);

  Future<UserModel> fetchMe() async {
    final response = await _apiClient.dio.get('/users/me');
    return UserModel.fromJson(response.data as Map<String, dynamic>);
  }

  Future<void> updatePreferences({
    required String budget,
    required String rhythm,
    required String groupType,
    required List<String> interests,
    String? religion,
  }) async {
    await _apiClient.dio.patch(
      '/users/preferences',
      data: {
        'budget': budget,
        'rhythm': rhythm,
        'groupType': groupType,
        'interests': interests,
        if (religion != null) 'religion': religion,
      },
    );
  }
}
