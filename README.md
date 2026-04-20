This was built in eclipse using tomcat 10.1

When launching the project for the first time go to the tomcat server tab and in launch configurations-->Environment create environment variables set to:
DB_URL = jdbc:postgresql://aws-1-us-east-2.pooler.supabase.com:5432/postgres?sslmode=require
DB_USER = postgres.epnpxylvitkfmlufbdqt
DB_PASSWORD = password is in the instagram GC

Setting these variables allows the DBConnectionManger to connect to the SupaBase database
