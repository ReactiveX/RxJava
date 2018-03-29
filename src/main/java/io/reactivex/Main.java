package io.reactivex;

import io.reactivex.functions.*;

public class Main {
    public static void main(String[] args) {
        Observable.just(0)
                .zipWith(Observable.just(1), Observable.just(2), new Function3<Integer, Integer, Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer, Integer integer2, Integer integer3) throws Exception {
                        System.out.print(" nje " + integer + " dy " + integer2 + " tre " + integer3);
                        return 1;
                    }
                }).subscribe();

        Observable.just(0)
                .zipWith(Observable.just(1), Observable.just(2), Observable.just(3), new Function4<Integer, Integer, Integer, Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer, Integer integer2, Integer integer3, Integer integer4) throws Exception {
                        System.out.print("\n nje " + integer + " dy " + integer2 + " tre " + integer3 + " kater " + integer4);
                        return 1;
                    }
                }).subscribe();
        Observable.just(0)
                .zipWith(Observable.just(1), Observable.just(2), Observable.just(3), Observable.just(4), new Function5<Integer, Integer, Integer, Integer, Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer, Integer integer2, Integer integer3, Integer integer4, Integer integer5) throws Exception {
                        System.out.print("\n nje " + integer + " dy " + integer2 + " tre " + integer3 + " kater " + integer4 + " pes " + integer5);
                        return 1;
                    }
                }).subscribe();
        Observable.just(0)
                .zipWith(Observable.just(1), Observable.just(2), Observable.just(3), Observable.just(4), Observable.just(5), new Function6<Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer, Integer integer2, Integer integer3, Integer integer4, Integer integer5, Integer integer6) throws Exception {
                        System.out.print("\n nje " + integer + " dy " + integer2 + " tre " + integer3 + " kater " + integer4 + " pes " + integer5 + " gjasht " + integer6);
                        return 1;
                    }
                }).subscribe();

        Observable.just(0)
                .zipWith(Observable.just(1), Observable.just(2), Observable.just(3), Observable.just(4), Observable.just(5), Observable.just(6), new Function7<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer, Integer integer2, Integer integer3, Integer integer4, Integer integer5, Integer integer6, Integer integer7) throws Exception {
                        System.out.print("\n nje " + integer + " dy " + integer2 + " tre " + integer3 + " kater " + integer4 + " pes " + integer5 + " gjasht " + integer6 + " shtat " + integer7);
                        return 1;
                    }
                }).subscribe();

        Observable.just(0)
                .zipWith(Observable.just(1), Observable.just(2), Observable.just(3), Observable.just(4), Observable.just(5), Observable.just(6), Observable.just(7), new Function8<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer, Integer integer2, Integer integer3, Integer integer4, Integer integer5, Integer integer6, Integer integer7, Integer integer8) throws Exception {
                        System.out.print("\n nje " + integer + " dy " + integer2 + " tre " + integer3 + " kater " + integer4 + " pes " + integer5 + " gjasht " + integer6 + " shtat " + integer7 + " tet " + integer8);
                        return 1;
                    }
                }).subscribe();
        Observable.just(0)
                .zipWith(Observable.just(1), Observable.just(2), Observable.just(3), Observable.just(4), Observable.just(5), Observable.just(6), Observable.just(7), Observable.just(8), new Function9<Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer, Integer>() {
                    @Override
                    public Integer apply(Integer integer, Integer integer2, Integer integer3, Integer integer4, Integer integer5, Integer integer6, Integer integer7, Integer integer8, Integer integer9) throws Exception {
                        System.out.print("\n nje " + integer + " dy " + integer2 + " tre " + integer3 + " kater " + integer4 + " pes " + integer5 + " gjasht " + integer6 + " shtat " + integer7 + " tet " + integer8 + " nent " + integer9);
                        return 1;
                    }
                }).subscribe();
    }
}
