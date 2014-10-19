package edu.upc.eetac.dsa.smachado.beeter.api;
	
	import javax.naming.Context;
	import javax.naming.InitialContext;
	import javax.naming.NamingException;
	import javax.sql.DataSource;
	 
	public class DataSourceSPA {
	    private DataSource dataSource;
		private static DataSourceSPA instance;//ref al singleton
	 
		private DataSourceSPA() {//singleton
			super();
			Context envContext = null;
			try {
				envContext = new InitialContext();//necesitamos el contexto del entorno
				Context initContext = (Context) envContext.lookup("java:/comp/env");
				dataSource = (DataSource) initContext.lookup("jdbc/beeterdb");//ref al datasource se obtiene
			} catch (NamingException e1) {
				e1.printStackTrace();
			}
		}
	 
		public final static DataSourceSPA getInstance() {//si la instancia es nula es por primera vez
			if (instance == null)
				instance = new DataSourceSPA();
			return instance;
		}
	 
		public DataSource getDataSource() {//metodo para obtener lo que nos interesa
			return dataSource;
		}
	}

