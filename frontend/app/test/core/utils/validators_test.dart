// Testes de CONTRATO para a futura função validateEmail (frontend) —
// validação client-side de formato de email, usada na tela de login/cadastro
// antes de qualquer chamada ao Supabase Auth.
//
// IMPORTANTE: esta task é TDD "testes primeiro" — lib/core/utils/validators.dart
// e a função validateEmail() ainda NÃO existem no código de produção. Diferente
// de um teste que apenas falharia em runtime, aqui não dá pra escrever um
// teste real que IMPORTA e CHAMA validateEmail(...): o Dart resolve/compila o
// arquivo de teste inteiro antes de decidir o que executar, então um símbolo
// não encontrado (import inexistente ou função inexistente) quebra a
// compilação do arquivo INTEIRO — inclusive de outros testes que nada têm a
// ver com isso. Isso é pior do que um teste explicitamente marcado como
// "skip" (que ainda reporta um resultado, só que pulado).
//
// Por isso este arquivo NÃO importa lib/core/utils/validators.dart. Cada caso
// de teste é declarado com test(...) real (então `flutter test` reporta a
// contagem de skipped corretamente), mas o corpo fica comentado — documentando
// em código o contrato exato (entrada -> saída esperada) que a implementação
// futura precisa satisfazer — e o parâmetro `skip` explica o motivo.
//
// Quando lib/core/utils/validators.dart existir com
// `String? validateEmail(String email)`, o processo é: adicionar
// `import 'package:app/core/utils/validators.dart';` no topo, descomentar o
// corpo de cada teste abaixo e remover o `skip`. Nesse momento os testes
// devem rodar de verdade (e falhar/red até a lógica estar correta).

import 'package:app/core/utils/validators.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('validateEmail — contrato (ainda não implementado)', () {
    test(
      'string vazia retorna "Email é obrigatório"',
      () {
        expect(validateEmail(''), 'Email é obrigatório');
      },
    );

    test(
      'string só com espaços retorna "Email é obrigatório"',
      () {
        expect(validateEmail('   '), 'Email é obrigatório');
      },
    );

    test(
      'sem @ (nenhum @ na string) retorna "Digite um email válido"',
      () {
        expect(validateEmail('naoeh_um_email'), 'Digite um email válido');
      },
    );

    test(
      'sem domínio depois do @ retorna "Digite um email válido"',
      () {
        expect(validateEmail('user@'), 'Digite um email válido');
      },
    );

    test(
      'sem parte local antes do @ retorna "Digite um email válido"',
      () {
        expect(validateEmail('@dominio.com'), 'Digite um email válido');
      },
    );

    test(
      'espaço no meio (sem @) retorna "Digite um email válido"',
      () {
        expect(validateEmail('user dominio.com'), 'Digite um email válido');
      },
    );

    test(
      'sem TLD depois do domínio retorna "Digite um email válido"',
      () {
        expect(validateEmail('user@dominio'), 'Digite um email válido');
      },
    );

    test(
      'email simples válido retorna null',
      () {
        expect(validateEmail('usuario@exemplo.com'), null);
      },
    );

    test(
      'email válido com +tag e subdomínio (multi-label TLD) retorna null',
      () {
        expect(
          validateEmail('usuario.teste+tag@sub.exemplo.com.br'),
          null,
        );
      },
    );
  });
}
