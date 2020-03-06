package server.service;

import org.apache.olingo.server.api.uri.queryoption.FilterOption;

public class MyFilterExpressionVisitor {

    public static String transformToQuery(FilterOption filterOption) {

		String query = filterOption.getExpression().toString();

		query = eliminarLlaves(query);
		query = transformBinaryOperatos(query);
		query = transformContainsOperators(query);

        return query;
	}

	private static String eliminarLlaves(String query) {
		query = query.replaceAll("\\[", "");
		query = query.replaceAll("\\]", "");
		query = query.replaceAll("\\{", "(");
		query = query.replaceAll("\\}", ")");
		return query;
	}

	private static String transformBinaryOperatos(String query) {
		query = query.replaceAll(" EQ ", " = ");
		query = query.replaceAll(" NE ", " <> ");
		query = query.replaceAll(" GE ", " >= ");
		query = query.replaceAll(" GT ", " > ");
		query = query.replaceAll(" LE ", " <= ");
		query = query.replaceAll(" LT ", " < ");
		query = query.replaceAll(" ADD ", " + ");
		query = query.replaceAll(" DIV ", " / ");
		query = query.replaceAll(" MOD ", " % ");
		query = query.replaceAll(" MUL ", " * ");
		query = query.replaceAll(" SUB ", " - ");

		return query;
	}

	private static String transformContainsOperators(String query) {

		int iniExt;
		int finExt;

		String property;
		String value;

		int ini;
		int separator;

		// contains == string
		// contains(Titulo,'Moon') => Titulo LIKE '%Moon%'

		while( (iniExt = query.indexOf("contains")) != -1 ) {

			ini = iniExt + "contains(".length();
			finExt = query.indexOf("')", ini) + 2;
			separator = query.indexOf(",", ini);

			property = query.substring(ini, separator);
			value = query.substring(separator + 2, finExt - 1);
			value = value.replaceAll("'", "");

			String contains = query.substring(iniExt, finExt - 1);

			String sql = property + " LIKE '%" + value + "%'";

			query = query.replace(contains, sql);

		}

		return query;
	}

}