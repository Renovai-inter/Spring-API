package com.renovai.api.repository;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EmpresaSchemaRepositoryTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("operacoes")
    void consultasVinculamExatamenteOsParametrosDoSql(Method operacao) throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class, invocation -> {
            String method = invocation.getMethod().getName();
            Object[] raw = invocation.getRawArguments();
            String sql = (String) raw[0];
            String sqlSemLiterais = sql.replaceAll("'(?:''|[^'])*'", "");
            long placeholders = sqlSemLiterais.chars().filter(c -> c == '?').count();
            int offset = method.equals("update") ? 1 : 2;
            Object[] parametros = raw.length > offset ? (Object[]) raw[offset] : new Object[0];
            assertThat(parametros.length).as("Parâmetros de %s", operacao.getName())
                    .isEqualTo((int) placeholders);
            if (method.equals("update")) return 1;
            if (method.equals("query")) return List.of();
            return null;
        });
        EmpresaSchemaRepository repository = new EmpresaSchemaRepository(jdbcTemplate);
        Object[] arguments = Arrays.stream(operacao.getParameterTypes()).map(type -> {
            if (type == UUID.class) return UUID.randomUUID();
            if (type == BigDecimal.class) return BigDecimal.ONE;
            if (type == Integer.class) return 5;
            if (type == String.class) return "valor";
            throw new IllegalArgumentException("Tipo não coberto: " + type);
        }).toArray();

        operacao.invoke(repository, arguments);
    }

    static Stream<Method> operacoes() {
        return Arrays.stream(EmpresaSchemaRepository.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .sorted(java.util.Comparator.comparing(Method::getName));
    }
}
