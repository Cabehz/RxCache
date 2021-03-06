package com.cabe.lib.cache.disk;

import android.util.Log;

import com.cabe.lib.cache.DiskCacheRepository;
import com.cabe.lib.cache.exception.DiskExceptionCode;
import com.cabe.lib.cache.exception.RxException;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.jakewharton.disklrucache.DiskLruCache;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import rx.android.BuildConfig;

/**
 * 磁盘缓存的实现类<br>
 * 使用DiskLruCache进行数据处理<br>
 * Created by cabe on 16/4/12.
 */
public class DiskCacheManager implements DiskCacheRepository {
    public static String DISK_CACHE_PATH = "";
    private static DiskLruCache cache;

    public DiskCacheManager(String cachePath) throws Exception {
        if(cache == null) {
            File cacheDir = new File(cachePath);
            if(!cacheDir.exists()) {
                boolean result = cacheDir.mkdirs();
                Log.d("DiskCacheManager", "mkdirs " + result);
            }
            cache = DiskLruCache.open(cacheDir, BuildConfig.VERSION_CODE, 1, 1024 * 1024 * 10);
        }
    }
    public DiskCacheManager() throws Exception {
        this(DISK_CACHE_PATH);
    }
    private String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }
    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
    private <T> String getTypeTokenKey(TypeToken<T> typeToken) {
        return typeToken == null ? "" : hashKeyForDisk(typeToken.toString());
    }
    public synchronized <T> boolean exits(TypeToken<T> typeToken){
        try {
            DiskLruCache.Snapshot snapshot = cache.get(getTypeTokenKey(typeToken));
            if(snapshot!=null){
                return true;
            }
        } catch (Exception e) {
            throw RxException.build(DiskExceptionCode.DISK_EXCEPTION_CHECK, e);
        }
        return false;
    }
    public synchronized <T> T get(TypeToken<T> typeToken){
        try {
            if(exits(typeToken)) {
                String data = cache.get(getTypeTokenKey(typeToken)).getString(0);
                return new Gson().fromJson(data, typeToken.getType());
            } else {
                return null;
            }
        }catch(Exception e){
            throw RxException.build(DiskExceptionCode.DISK_EXCEPTION_GET, e);
        }
    }
    public synchronized <T> boolean put(TypeToken<T> typeToken, T obj){
        if(typeToken == null) {
            throw RxException.build(DiskExceptionCode.DISK_EXCEPTION_TYPE, null);
        }
        try {
            if(obj != null) {
                String json = new Gson().toJson(obj);
                DiskLruCache.Editor editor = cache.edit(getTypeTokenKey(typeToken));
                editor.set(0, json);
                editor.commit();
            }
        } catch (Exception e) {
            throw RxException.build(DiskExceptionCode.DISK_EXCEPTION_SAVE, e);
        }
        return true;
    }
}