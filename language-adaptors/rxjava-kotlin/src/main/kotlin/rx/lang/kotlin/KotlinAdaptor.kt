package rx.lang.kotlin

import rx.util.functions.FunctionLanguageAdaptor

public class KotlinAdaptor: FunctionLanguageAdaptor {

    public override fun call(function: Any?, args: Array<out Any>?): Any? {
        return when(args!!.size){
            0 -> (function!! as Function0<Any>)()
            1 -> (function!! as Function1<Any, Any>)(args[0])
            2 -> (function!! as Function2<Any, Any, Any>)(args[0], args[1])
            3 -> (function!! as Function3<Any, Any, Any, Any>)(args[0], args[1], args[2])
            4 -> (function!! as Function4<Any, Any, Any, Any, Any>)(args[0], args[1], args[2], args[3])
            5 -> (function!! as Function5<Any, Any, Any, Any, Any, Any>)(args[0], args[1], args[2], args[3], args[4])
            6 -> (function!! as Function6<Any, Any, Any, Any, Any, Any, Any>)(args[0], args[1], args[2], args[3], args[4], args[5])
            7 -> (function!! as Function7<Any, Any, Any, Any, Any, Any, Any, Any>)(args[0], args[1], args[2], args[3], args[4], args[5], args[6])
            8 -> (function!! as Function8<Any, Any, Any, Any, Any, Any, Any, Any, Any>)(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7])
            9 -> (function!! as Function9<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>)(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8])
            10 -> (function!! as Function10<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>)(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9])
            11 -> (function!! as Function11<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>)(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10])
            12 -> (function!! as Function12<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>)(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11])
            13 -> (function!! as Function13<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>)(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12])
            14 -> (function!! as Function14<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>)(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13])
            15 -> (function!! as Function15<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>)(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14])
            16 -> (function!! as Function16<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>)(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15])
            17 -> (function!! as Function17<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>)(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16])
            18 -> (function!! as Function18<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>)(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17])
            19 -> (function!! as Function19<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>)(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18])
            20 -> (function!! as Function20<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>)(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18], args[19])
            21 -> (function!! as Function21<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>)(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18], args[19], args[20])
            22 -> (function!! as Function22<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>)(args[0], args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8], args[9], args[10], args[11], args[12], args[13], args[14], args[15], args[16], args[17], args[18], args[19], args[20], args[21])
            else -> throw UnsupportedOperationException("")
        }
    }

    public override fun getFunctionClass(): Array<Class<out Any?>>? {
        return array(
                javaClass<Function0<Any>>(),
                javaClass<Function1<Any, Any>>(),
                javaClass<Function2<Any, Any, Any>>(),
                javaClass<Function3<Any, Any, Any, Any>>(),
                javaClass<Function4<Any, Any, Any, Any, Any>>(),
                javaClass<Function5<Any, Any, Any, Any, Any, Any>>(),
                javaClass<Function6<Any, Any, Any, Any, Any, Any, Any>>(),
                javaClass<Function7<Any, Any, Any, Any, Any, Any, Any, Any>>(),
                javaClass<Function8<Any, Any, Any, Any, Any, Any, Any, Any, Any>>(),
                javaClass<Function9<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>>(),
                javaClass<Function10<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>>(),
                javaClass<Function11<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>>(),
                javaClass<Function12<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>>(),
                javaClass<Function13<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>>(),
                javaClass<Function14<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>>(),
                javaClass<Function15<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>>(),
                javaClass<Function16<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>>(),
                javaClass<Function17<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>>(),
                javaClass<Function18<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>>(),
                javaClass<Function19<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>>(),
                javaClass<Function20<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>>(),
                javaClass<Function21<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>>(),
                javaClass<Function22<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any>>())
    }
}