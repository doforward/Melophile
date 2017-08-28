package com.vpaliy.data.source.local;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import static com.vpaliy.data.source.local.MusicContract.Users;
import static com.vpaliy.data.source.local.MusicContract.Playlists;
import static com.vpaliy.data.source.local.MusicContract.Tracks;
import static com.vpaliy.data.source.local.MusicDatabaseHelper.Tables;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class MusicProvider extends ContentProvider{

    private MusicUriMatcher matcher;
    private MusicDatabaseHelper database;

    @Override
    public boolean onCreate() {
        if(getContext()!=null){
            this.database=new MusicDatabaseHelper(getContext());
            this.matcher=new MusicUriMatcher();
        }
        return database!=null;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        final SQLiteDatabase db=database.getReadableDatabase();
        final MusicMatchEnum matchEnum=matcher.match(uri);

        SqlQueryBuilder builder=buildQuery(uri,matchEnum);
        Cursor cursor=builder
                .where(selection,selectionArgs)
                .query(db,projection,sortOrder);

        Context context = getContext();
        if (null != context) {
            cursor.setNotificationUri(context.getContentResolver(), uri);
        }
        return cursor;
    }


    private void notifyChange(Uri uri) {
        if (getContext()!=null) {
            Context context = getContext();
            context.getContentResolver().notifyChange(uri, null);
        }
    }

    private void deleteDatabase(){
        database.close();
        Context context=getContext();
        if(context!=null){
            MusicDatabaseHelper.deleteDatabase(context);
            database=new MusicDatabaseHelper(context);
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return matcher.getType(uri);
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if(values==null) {
            throw new IllegalArgumentException("Values are null");
        }
        final SQLiteDatabase db=database.getWritableDatabase();
        final MusicMatchEnum matchEnum=matcher.match(uri);
        if(matchEnum.table!=null){
            db.insertWithOnConflict(matchEnum.table,null,values,SQLiteDatabase.CONFLICT_REPLACE);
            notifyChange(uri);
        }
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        if(uri==MusicContract.BASE_CONTENT_URI){
            deleteDatabase();
            notifyChange(uri);
            return 1;
        }
        final SQLiteDatabase db=database.getWritableDatabase();
        final MusicMatchEnum matchEnum=matcher.match(uri);
        final SqlQueryBuilder builder=buildQuery(uri,matchEnum);

        int result=builder.where(selection,selectionArgs).delete(db);
        notifyChange(uri);
        return result;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        final SQLiteDatabase db=database.getWritableDatabase();
        final MusicMatchEnum matchEnum=matcher.match(uri);
        final SqlQueryBuilder builder=buildQuery(uri,matchEnum);

        int result=builder.where(selection,selectionArgs).update(db,values);
        notifyChange(uri);
        return result;
    }

    private SqlQueryBuilder buildQuery(Uri uri, MusicMatchEnum matchEnum){
        SqlQueryBuilder builder=new SqlQueryBuilder();
        String id;
        switch (matchEnum){
            case PLAYLISTS:
                return builder.table(Tables.PLAYLISTS);
            case TRACKS:
                return builder.table(Tables.TRACKS);
            case USERS:
                return builder.table(Tables.USERS);
            case HISTORY_TRACKS:
                return builder.table(Tables.HISTORY_TRACK);
            case HISTORY_PLAYLISTS:
                return builder.table(Tables.HISTORY_PLAYLIST);
            case ME:
                return builder.table(Tables.ME);
            case PLAYLIST:
                id=Playlists.getPlaylistId(uri);
                return builder.table(Tables.PLAYLISTS)
                        .where(Playlists.PLAYLIST_ID+"=?",id);
            case TRACK:
                id=Tracks.getTrackId(uri);
                return builder.table(Tables.TRACKS)
                        .where(Tracks.TRACK_ID+"=?",id);
            case USER:
                id=Users.getUserId(uri);
                return builder.table(Tables.USERS)
                        .where(Users.USER_ID+"=?",id);
            case PLAYLIST_TRACKS:
                id=Playlists.getPlaylistId(uri);
                return builder.table(Tables.PLAYLIST_JOIN_TRACKS)
                        .mapToTable(Playlists.PLAYLIST_ID,Tables.PLAYLISTS)
                        .mapToTable(Tracks.TRACK_ID,Tables.TRACKS)
                        .where(Playlists.PLAYLIST_ID+"=?",id);
            case USER_LIKED_TRACKS:
                id=Users.getUserId(uri);
                return builder.table(Tables.USER_JOIN_LIKED_TRACKS)
                        .mapToTable(Users.USER_ID,Tables.USERS)
                        .mapToTable(Tracks.TRACK_ID,Tables.TRACKS)
                        .where(Users.USER_ID+"=?",id);
            case USER_TRACKS:
                id=Users.getUserId(uri);
                return builder.table(Tables.USER_JOIN_TRACKS)
                        .mapToTable(Users.USER_ID,Tables.USERS)
                        .mapToTable(Tracks.TRACK_ID,Tables.TRACKS)
                        .where(Users.USER_ID+"=?",id);
            case USER_PLAYLISTS:
                id=Users.getUserId(uri);
                return builder.table(Tables.USER_JOIN_PLAYLISTS)
                        .mapToTable(Users.USER_ID,Tables.USERS)
                        .mapToTable(Playlists.PLAYLIST_ID,Tables.PLAYLISTS)
                        .where(Users.USER_ID+"=?",id);
            default:
                throw new IllegalArgumentException("Wrong matcher!");
        }
    }
}
