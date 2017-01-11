package com.wangliang170111.rxbus;


import android.util.Log;

import java.util.HashMap;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subjects.ReplaySubject;
import rx.subjects.SerializedSubject;
import rx.subscriptions.CompositeSubscription;

/**
 * Created by wangliang on 2016/12/30.
 */

public class RxBus {

    private HashMap<String , CompositeSubscription> mSubscriptions;

    /*事件总线，具有观察者和被观察者的双重身份*/
    private final SerializedSubject<Object , Object> serializedSubject = new SerializedSubject<>(ReplaySubject.create());

    private RxBus(){}
    private static class RxBusInner{
        private static RxBus rxBus = new RxBus();
    }
    ConnectableObservable co;

    /**
     * @return a unique RxBus object.
     */
    public static RxBus getIntance(){
        if(RxBusInner.rxBus == null){
            RxBusInner.rxBus = new RxBus();
            return RxBusInner.rxBus;
        }else return RxBusInner.rxBus;
    }

    /**
     * @param object 要发送的数据
     */
    public void sendObject(String tag , Object object){
        T t = new T();
        t.setTag(tag);
        t.setObject(object);
        serializedSubject.onNext(t);
    }

    /**
     * @param tag
     * @param action1
     * @param error
     * 该方法为自动记录了注册信息，如果不需要切换线程可用该方法
     */
    public  void doSubscribe(String tag , Action1<Object> action1 , Action1<Throwable> error){
        addSubscription(tag , getObservable(tag).subscribe(action1 , error));
    }

    /**
     * 该方法可以获取到Observable对象，如果需要做map操作可以用该方法；使用该方法需要自己执行<addSubscription>方法
     * @return
     */
    public Observable<Object> getObservable(final String tag){
        Observable observable =  serializedSubject.filter(new Func1<Object, Boolean>() {

            @Override
            public Boolean call(Object o) {
                if(o instanceof T){
                    if(((T)o).getTag().equals(tag)){
                        return true;
                    }
                }
                return false;
            }
        }).map(new Func1<Object, Object>() {
            @Override
            public Object call(Object o) {
                return ((T)o).getObject();
            }
        });
        return observable;
    }

    /**
     * @param tag
     * @param subscription
     * 保存订阅信息
     */
    public void addSubscription(String tag , Subscription subscription){
        if(mSubscriptions == null)
            mSubscriptions = new HashMap<>();
        if(mSubscriptions.containsKey(tag)){
            mSubscriptions.get(tag).add(subscription);
        }else {
            CompositeSubscription compositeSubscription = new CompositeSubscription();
            compositeSubscription.add(subscription);
            mSubscriptions.put(tag , compositeSubscription);
            Log.d("outt" , "已经添加");
        }
    }

    public void unSubscribe(final String tag){

        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                if(mSubscriptions == null)         //判断是否为空
                    subscriber.onCompleted();
                else subscriber.onNext(tag);
            }
        }).filter(new Func1<String , Boolean>() {  //判断是否已包含
            @Override
            public Boolean call(String tag) {
                if(mSubscriptions.containsKey(tag))
                    return true;
                else return false;
            }
        }).subscribe(new Action1<String>() {    //取消注册并移除记录
            @Override
            public void call(String s) {
                CompositeSubscription compositeSubscription = mSubscriptions.get(s);
                if(!compositeSubscription.isUnsubscribed()) {
                    compositeSubscription.unsubscribe();
                    mSubscriptions.remove(s);
                    Log.d("outt" , "已经移除");
                }
            }
        });
    }

    /**
     * @return if has observer(true),else false.
     */
    public boolean hasObserver(){
        return serializedSubject.hasObservers();
    }

    /*为了给数据添加上tag标记*/
    class T {
        private Object object;
        private String tag;

        public Object getObject() {
            return object;
        }

        public void setObject(Object object) {
            this.object = object;
        }

        public String getTag() {
            return tag;
        }

        public void setTag(String tag) {
            this.tag = tag;
        }

        public T() {
        }
    }


}
