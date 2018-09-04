package rocks.voss.androidutils.utils;

import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class DatabaseUtil {
    @Setter
    @Getter
    private static Database database;

    public void openDatabase(Context context, Class dbClazz, String dbName, Migration... migrations) {
        RoomDatabase.Builder databaseBuilder = Room.databaseBuilder(context, dbClazz, dbName);
        if (migrations != null || migrations.length < 1) {
            databaseBuilder.addMigrations(migrations);
        } else {
            databaseBuilder.fallbackToDestructiveMigration();
        }
        database = (Database) databaseBuilder.build();
    }

    public <DaoType> DaoType getDao(Class clazz) {
        if (database == null) {
            return null;
        }
        return database.getDao(clazz);
    }

    public <ELEMENT> ELEMENT insert(Class<ELEMENT> daoType, ELEMENT element) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            throw new IllegalStateException("This method is only available with SDK >= 26");
        }
        Thread thread = new Thread() {
            @Override
            public void run() {
                super.run();
                Object daoObject = getDao(daoType);
                try {
                    for (Method method : daoObject.getClass().getDeclaredMethods()) {
                        if (method.getName().equals("insert") && method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(element.getClass())) {
                            method.invoke(daoObject, element);
                            return;
                        }
                    }
                } catch (IllegalAccessException e) {
                    Log.e(this.getClass().toString(), "IllegalAccessException", e);
                } catch (InvocationTargetException e) {
                    Log.e(this.getClass().toString(), "InvocationTargetException", e);
                }
            }
        };
        thread.start();
        return null;
    }

    public void getAll(Class daoType, Object primaryKey, GetAllCallback callback) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            throw new IllegalStateException("This method is only available with SDK >= 26");
        }
        Thread thread = new Thread() {
            @Override
            public void run() {
                super.run();

                Object daoObject = getDao(daoType);
                try {
                    for (Method method : daoObject.getClass().getDeclaredMethods()) {
                        if (method.getName().equals("getAll") && method.getParameterCount() == 1) {
                            List<Object> result;
                            if (primaryKey == null) {
                                result = (List<Object>) method.invoke(daoObject);
                            } else {
                                result = (List<Object>) method.invoke(daoObject, primaryKey);
                            }
                            callback.onResultReady(result);
                            return;
                        }
                    }
                } catch (IllegalAccessException e) {
                    Log.e(this.getClass().toString(), "IllegalAccessException", e);
                } catch (InvocationTargetException e) {
                    Log.e(this.getClass().toString(), "InvocationTargetException", e);
                }
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Log.e(this.getClass().toString(), "InterruptedException", e);
        }
    }

    public void delete(Class daoType, Object primaryKey) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                super.run();

                Object daoObject = getDao(daoType);
                try {
                    for (Method method : daoObject.getClass().getDeclaredMethods()) {
                        if (method.getName().equals("delete") && method.getParameterCount() == 1) {
                            method.invoke(daoObject, primaryKey);
                            return;
                        }
                    }
                } catch (IllegalAccessException e) {
                    Log.e(this.getClass().toString(), "IllegalAccessException", e);
                } catch (InvocationTargetException e) {
                    Log.e(this.getClass().toString(), "InvocationTargetException", e);
                }
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            Log.e(this.getClass().toString(), "InterruptedException", e);
        }
    }

    public interface GetAllCallback {
        void onResultReady(List<Object> elements);
    }

    public interface Database {
        <DaoObject> DaoObject getDao(Class daoElement);
    }
}
