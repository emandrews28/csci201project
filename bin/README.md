This was built in eclipse using tomcat 10.1

When launching the project for the first time go to the tomcat server tab and in launch configurations-->Environment create environment variables set to:
DB_URL = `jdbc:postgresql://aws-1-us-east-2.pooler.supabase.com:5432/postgres?sslmode=require`
DB_USER = `postgres.epnpxylvitkfmlufbdqt`
DB_PASSWORD = `password is in the instagram GC`

Setting these variables allows the DBConnectionManger to connect to the SupaBase database

## Photo uploads

Before the photo-upload feature will work, run `sql/photos.sql` once against the Supabase database (paste it into the Supabase SQL editor or run `psql -f sql/photos.sql`). It creates the `photos` and `photo_tags` tables with FKs to `users`, `restaurants`, and `reviews`.

Optionally add one more environment variable to the Tomcat launch config to control where uploaded images are written on disk. If unset, it defaults to `~/csci201_photos`:

PHOTO_UPLOAD_DIR = /absolute/path/to/upload/dir

Images are served back to the browser through `/photos/files/*` (see `PhotoFileServlet`) — nothing in the webapp directory needs to change.
