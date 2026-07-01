import { api } from "@/lib/api";
import { setToken, clearToken } from "@/lib/auth";

interface AuthResponse {
  token: string;
  user: { id: string; name: string; email: string; onboardingCompleted: boolean };
}

export const authService = {
  async signup(name: string, email: string, password: string): Promise<AuthResponse> {
    const { data } = await api.post<AuthResponse>("/auth/signup", { name, email, password });
    setToken(data.token);
    return data;
  },

  async login(email: string, password: string): Promise<AuthResponse> {
    const { data } = await api.post<AuthResponse>("/auth/login", { email, password });
    setToken(data.token);
    return data;
  },

  logout(): void {
    clearToken();
  },
};
