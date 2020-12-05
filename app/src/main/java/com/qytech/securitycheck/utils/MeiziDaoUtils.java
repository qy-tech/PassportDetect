package com.qytech.securitycheck.utils;

import android.content.Context;
import android.util.Log;

import com.qutech.daodemo.greenDao.DbBeanDao;
import com.qytech.securitycheck.db.DaoManager;
import com.qytech.securitycheck.db.DbBean;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

public class MeiziDaoUtils {
    private static final String TAG = MeiziDaoUtils.class.getSimpleName();
    private DaoManager mManager;

    public MeiziDaoUtils(Context context){
        mManager = DaoManager.getInstance();
        mManager.init(context);
    }

    /**
     * 完成meizi记录的插入，如果表未创建，先创建Meizi表
     * @param meizi
     * @return
     */
    public boolean insertMeizi(DbBean meizi){
        boolean flag = false;
        flag = mManager.getDaoSession().getDbBeanDao().insert(meizi) == -1 ? false : true;
        Log.i(TAG, "insert Meizi :" + flag + "-->" + meizi.toString());
        return flag;
    }

    /**
     * 插入多条数据，在子线程操作
     * @param meiziList
     * @return
     */
    public boolean insertMultMeizi(final List<DbBean> meiziList) {
        boolean flag = false;
        try {
            mManager.getDaoSession().runInTx(new Runnable() {
                @Override
                public void run() {
                    for (DbBean meizi : meiziList) {
                        mManager.getDaoSession().insertOrReplace(meizi);
                    }
                }
            });
            flag = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 修改一条数据
     * @param meizi
     * @return
     */
    public boolean updateMeizi(DbBean meizi){
        boolean flag = false;
        try {
            mManager.getDaoSession().update(meizi);
            flag = true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 删除单条记录
     * @param meizi
     * @return
     */
    public boolean deleteMeizi(DbBean meizi){
        boolean flag = false;
        try {
            //按照id删除
            mManager.getDaoSession().delete(meizi);
            flag = true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 删除所有记录
     * @return
     */
    public boolean deleteAll(){
        boolean flag = false;
        try {
            //按照id删除
            mManager.getDaoSession().deleteAll(DbBean.class);
            flag = true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 查询所有记录
     * @return
     */
    public  List<DbBean> queryAllMeizi(){
        return mManager.getDaoSession().loadAll(DbBean.class);
    }

    /**
     * 根据主键id查询记录
     * @param key
     * @return
     */
    public DbBean queryMeiziById(long key){
        return mManager.getDaoSession().load(DbBean.class, key);
    }

    /**
     * 使用native sql进行查询操作
     */
    public List<DbBean> queryMeiziByNativeSql(String sql, String[] conditions){
        return mManager.getDaoSession().queryRaw(DbBean.class, sql, conditions);
    }

    /**
     * 使用queryBuilder进行查询
     * @return
     */
    public List<DbBean> queryMeiziByQueryBuilder(long id){
        QueryBuilder<DbBean> queryBuilder = mManager.getDaoSession().queryBuilder(DbBean.class);
        return queryBuilder.where(DbBeanDao.Properties.Id.eq(id)).list();
    }
}
