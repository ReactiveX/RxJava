import com.google.j2objc.annotations.AutoreleasePool;

import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import co.touchlab.doppel.testing.DopplJunitTestRunner;
import rx.internal.operators.InternalSafeSubscriberTest;
import rx.internal.operators.OnSubscribeCollectTest;
import rx.internal.operators.OnSubscribeRefCountTest;
import rx.internal.operators.OperatorCastTest;
import rx.internal.operators.OperatorCountTest;
import rx.internal.schedulers.InternalGenericScheduledExecutorServiceTest;
import rx.subjects.ReplaySubjectBoundedConcurrencyTest;

/*-[
#import <mach/mach.h>
]-*/

/**
 * Created by kgalligan on 10/12/16.
 */

public class OneTest
{
    public static List<String> allTestClassnames()
    {
        List<String> allClassnames = new ArrayList<>();

        loadClasses(allClassnames, bigmem);

        for(String allClassname : allClassnames)
        {
            System.out.println(allClassname);
        }
        return allClassnames;
    }

    private static void loadClasses(List<String> allClassnames, Class[] batch0)
    {
        for(Class cl : batch0)
        {
            allClassnames.add(cl.getCanonicalName());
        }
    }

    public static void runTests()
    {
//        runDoppl();
        new Thread()
        {
            @Override
            public void run()
            {
                runDoppl();
            }
        }.start();
    }

    @AutoreleasePool
    private static void runDoppl()
    {
        List<Class> smoothClasses = new ArrayList<>(Arrays.asList(alltests));
        smoothClasses.removeAll(Arrays.asList(bigmem));
        smoothClasses.removeAll(Arrays.asList(failing));

        Class[] asdf = smoothClasses.toArray(new Class[smoothClasses.size()]);

        DopplJunitTestRunner.run(asdf,
                new BigMemRunListener());

//        DopplJunitTestRunner.run(new Class[]{OperatorCountTest.class},
//                new RunListener());

//        DopplJunitTestRunner.run(loadClassList(TestResources.fulllist), new RunListener());
    }

    public static void runTests(String a)
    {
        run(a);
    }

    public static void runTests(String a, String b)
    {
        run(a, b);
    }

    public static void runTests(String a, String b, String c)
    {
        run(a, b, c);
    }

    public static void runTests(String a, String b, String c, String d)
    {
        run(a, b, c, d);
    }

    private static void run(String... classNames)
    {
        DopplJunitTestRunner.run(classNames, new BigMemRunListener());
    }

    /*public static void runMethod(String className, String methodName)
    {
        try
        {
            DopplJunitTestRunner.runMethod(Class.forName(className),
                    methodName,
                    new BigMemRunListener());
        }
        catch(ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }*/

    static class BigMemRunListener extends RunListener
    {
        long memSize;

        @Override
        public void testStarted(Description description) throws Exception
        {
            super.testStarted(description);
            System.out.println("TRACE Starting "+ description.getClassName() + "-" + description.getMethodName() );
            memSize = printMem();
        }

        @Override
        public void testFinished(Description description) throws Exception
        {
            super.testFinished(description);
            System.out.println("TRACE Finished "+ description.getClassName() + "-" + description.getMethodName() );
            long endSize = printMem();

            long megs = (long)Math.floor((double)(endSize-memSize)/(double)(1024*1024));
            if(Math.abs(megs) > 0)
            {
                System.out.println(
                        "ZZZZ: " + description.getClassName() + "-" + description.getMethodName() + " diff: " +
                                megs +"m");
            }
        }
    }

    private static native long printMem()/*-[
    struct task_basic_info info;
  mach_msg_type_number_t size = sizeof(info);
  kern_return_t kerr = task_info(mach_task_self(),
                                 TASK_BASIC_INFO,
                                 (task_info_t)&info,
                                 &size);
  if( kerr == KERN_SUCCESS ) {
    return info.resident_size;
  } else {
    return 0;
  }
    ]-*/;

    private static Class[] loadClassList(String fileData)
    {
        try
        {
            BufferedReader bufferedReader = new BufferedReader(new StringReader(fileData));
            String line;
            List<Class> classList = new ArrayList<>();

            while((line = bufferedReader.readLine()) != null)
            {
                if(line.endsWith(".java"))
                {
                    String className = line.substring(0, line.lastIndexOf(".java")).replace('/', '.');
                    classList.add(Class.forName(className));
                }
            }

            return classList.toArray(new Class[classList.size()]);
        }
        catch(Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static Class[] alltests = new Class[] {
            rx.BackpressureTests.class,
            rx.CombineLatestTests.class,
            rx.ConcatTests.class,
            rx.CovarianceTest.class,
            rx.ErrorHandlingTests.class,
            rx.GroupByTests.class,
            rx.MergeTests.class,
            rx.NotificationTest.class,
            rx.ObservableDoOnTest.class,
            rx.ObservableTests.class,
            rx.ObservableWindowTests.class,
            rx.ReduceTests.class,
            rx.ScanTests.class,
            rx.SingleTest.class,
            rx.StartWithTests.class,
            rx.SubscriberTest.class,
            rx.ThrottleLastTests.class,
            rx.ThrottleWithTimeoutTests.class,
            rx.ZipTests.class,
            rx.exceptions.CompositeExceptionTest.class,
            rx.exceptions.ExceptionsNullTest.class,
            rx.exceptions.OnNextValueTest.class,
            rx.functions.ActionsTest.class,
            rx.functions.FunctionsTest.class,
            rx.internal.operators.BackpressureUtilsTest.class,
            rx.internal.operators.BlockingOperatorLatestTest.class,
            rx.internal.operators.BlockingOperatorMostRecentTest.class,
            rx.internal.operators.BlockingOperatorNextTest.class,
            rx.internal.operators.BlockingOperatorToFutureTest.class,
            rx.internal.operators.BlockingOperatorToIteratorTest.class,
            rx.internal.operators.CachedObservableTest.class,
            rx.internal.operators.NotificationLiteTest.class,
            rx.internal.operators.OnSubscribeAmbTest.class,
            rx.internal.operators.OnSubscribeCombineLatestTest.class,
            rx.internal.operators.OnSubscribeCompletableTest.class,
            rx.internal.operators.OnSubscribeConcatDelayErrorTest.class,
            rx.internal.operators.OnSubscribeDeferTest.class,
            rx.internal.operators.OnSubscribeDelaySubscriptionOtherTest.class,
            rx.internal.operators.OnSubscribeDetachTest.class,
            rx.internal.operators.OnSubscribeFlattenIterableTest.class,
            rx.internal.operators.OnSubscribeFromArrayTest.class,
            rx.internal.operators.OnSubscribeFromCallableTest.class,
            rx.internal.operators.OnSubscribeFromIterableTest.class,
            rx.internal.operators.OnSubscribeGroupJoinTest.class,
            rx.internal.operators.OnSubscribeJoinTest.class,
            rx.internal.operators.OnSubscribeRangeTest.class,
            rx.internal.operators.OnSubscribeRefCountTest.class,
            rx.internal.operators.OnSubscribeSingleTest.class,
            rx.internal.operators.OnSubscribeTimerTest.class,
            rx.internal.operators.OnSubscribeToObservableFutureTest.class,
            rx.internal.operators.OnSubscribeUsingTest.class,
            rx.internal.operators.OperatorAllTest.class,
            rx.internal.operators.OperatorAnyTest.class,
            rx.internal.operators.OperatorAsObservableTest.class,
            rx.internal.operators.OperatorBufferTest.class,
            rx.internal.operators.OperatorCastTest.class,
            rx.internal.operators.OperatorConcatTest.class,
            rx.internal.operators.OperatorDebounceTest.class,
            rx.internal.operators.OperatorDefaultIfEmptyTest.class,
//            rx.internal.operators.OperatorDelayTest.class,
            rx.internal.operators.OperatorDematerializeTest.class,
            rx.internal.operators.OperatorDistinctTest.class,
            rx.internal.operators.OperatorDistinctUntilChangedTest.class,
            rx.internal.operators.OperatorDoAfterTerminateTest.class,
            rx.internal.operators.OperatorDoOnRequestTest.class,
            rx.internal.operators.OperatorDoOnSubscribeTest.class,
            rx.internal.operators.OperatorDoOnUnsubscribeTest.class,
            rx.internal.operators.OperatorEagerConcatMapTest.class,
            rx.internal.operators.OperatorElementAtTest.class,
            rx.internal.operators.OperatorFirstTest.class,
            rx.internal.operators.OperatorFlatMapTest.class,
            rx.internal.operators.OperatorGroupByTest.class,
            rx.internal.operators.OperatorIgnoreElementsTest.class,
            rx.internal.operators.OperatorLastTest.class,
            rx.internal.operators.OperatorMapNotificationTest.class,
            rx.internal.operators.OperatorMapPairTest.class,
            rx.internal.operators.OperatorMaterializeTest.class,
            rx.internal.operators.OperatorMergeDelayErrorTest.class,
            rx.internal.operators.OperatorMergeMaxConcurrentTest.class,
            rx.internal.operators.OperatorMergeTest.class,
            rx.internal.operators.OperatorMulticastTest.class,
            rx.internal.operators.OperatorObserveOnTest.class,
            rx.internal.operators.OperatorOnBackpressureBufferTest.class,
            rx.internal.operators.OperatorOnBackpressureDropTest.class,
            rx.internal.operators.OperatorOnBackpressureLatestTest.class,
            rx.internal.operators.OperatorOnErrorResumeNextViaFunctionTest.class,
            rx.internal.operators.OperatorOnErrorResumeNextViaObservableTest.class,
            rx.internal.operators.OperatorOnErrorReturnTest.class,
            rx.internal.operators.OperatorOnExceptionResumeNextViaObservableTest.class,
            rx.internal.operators.OperatorPublishFunctionTest.class,
            rx.internal.operators.OperatorPublishTest.class,
            rx.internal.operators.OperatorRepeatTest.class,
            rx.internal.operators.OperatorReplayTest.class,
            rx.internal.operators.OperatorRetryTest.class,
            rx.internal.operators.OperatorRetryWithPredicateTest.class,
            rx.internal.operators.OperatorSampleTest.class,
            rx.internal.operators.OperatorScanTest.class,
            rx.internal.operators.OperatorSequenceEqualTest.class,
            rx.internal.operators.OperatorSerializeTest.class,
            rx.internal.operators.OperatorSingleTest.class,
            rx.internal.operators.OperatorSkipLastTest.class,
            rx.internal.operators.OperatorSkipLastTimedTest.class,
            rx.internal.operators.OperatorSkipTest.class,
            rx.internal.operators.OperatorSkipTimedTest.class,
            rx.internal.operators.OperatorSkipUntilTest.class,
            rx.internal.operators.OperatorSkipWhileTest.class,
            rx.internal.operators.OperatorSubscribeOnTest.class,
            rx.internal.operators.OperatorSwitchIfEmptyTest.class,
            rx.internal.operators.OperatorSwitchTest.class,
            rx.internal.operators.OperatorTakeLastOneTest.class,
            rx.internal.operators.OperatorTakeLastTest.class,
            rx.internal.operators.OperatorTakeLastTimedTest.class,
            rx.internal.operators.OperatorTakeTest.class,
            rx.internal.operators.OperatorTakeTimedTest.class,
            rx.internal.operators.OperatorTakeUntilPredicateTest.class,
            rx.internal.operators.OperatorTakeUntilTest.class,
            rx.internal.operators.OperatorTakeWhileTest.class,
            rx.internal.operators.OperatorThrottleFirstTest.class,
            rx.internal.operators.OperatorTimeIntervalTest.class,
            rx.internal.operators.OperatorTimeoutTests.class,
            rx.internal.operators.OperatorTimeoutWithSelectorTest.class,
            rx.internal.operators.OperatorTimestampTest.class,
            rx.internal.operators.OperatorToObservableListTest.class,
            rx.internal.operators.OperatorToObservableSortedListTest.class,
            rx.internal.operators.OperatorUnsubscribeOnTest.class,
            rx.internal.operators.OperatorWindowWithObservableTest.class,
            rx.internal.operators.OperatorWindowWithSizeTest.class,
            rx.internal.operators.OperatorWindowWithStartEndObservableTest.class,
            rx.internal.operators.OperatorWindowWithTimeTest.class,
            rx.internal.operators.OperatorWithLatestFromTest.class,
            rx.internal.operators.OperatorZipCompletionTest.class,
            rx.internal.operators.OperatorZipIterableTest.class,
            rx.internal.operators.SingleDoAfterTerminateTest.class,
            rx.internal.operators.SingleOnSubscribeDelaySubscriptionOtherTest.class,
            rx.internal.operators.SingleOnSubscribeUsingTest.class,
            rx.internal.producers.ProducersTest.class,
            rx.schedulers.GenericScheduledExecutorServiceTest.class,
            rx.internal.schedulers.NewThreadWorkerTest.class,
            rx.internal.util.BlockingUtilsTest.class,
            rx.internal.util.IndexedRingBufferTest.class,
            rx.internal.util.JCToolsQueueTests.class,
            rx.internal.util.LinkedArrayListTest.class,
            rx.internal.util.OpenHashSetTest.class,
            rx.internal.util.RxRingBufferSpmcTest.class,
            rx.internal.util.RxRingBufferSpscTest.class,
            rx.internal.util.RxRingBufferWithoutUnsafeTest.class,
            rx.internal.util.ScalarSynchronousObservableTest.class,
            rx.internal.util.ScalarSynchronousSingleTest.class,
            rx.internal.util.SubscriptionListTest.class,
            rx.internal.util.SynchronizedQueueTest.class,
            rx.observables.BlockingObservableTest.class,
            rx.observables.ConnectableObservableTest.class,
            rx.observers.ObserversTest.class,
            rx.observers.SafeObserverTest.class,
            rx.observers.SafeSubscriberTest.class,
            rx.observers.SubscribersTest.class,
            rx.observers.TestObserverTest.class,
            rx.observers.TestSubscriberTest.class,
            rx.plugins.RxJavaPluginsTest.class,
            rx.plugins.RxJavaSchedulersHookTest.class,
            rx.schedulers.ComputationSchedulerTests.class,
            rx.schedulers.ImmediateSchedulerTest.class,
            rx.schedulers.IoSchedulerTest.class,
            rx.schedulers.NewThreadSchedulerTest.class,
            rx.schedulers.ResetSchedulersTest.class,
            rx.schedulers.TestSchedulerTest.class,
            rx.schedulers.TrampolineSchedulerTest.class,
            rx.singles.BlockingSingleTest.class,
            rx.subjects.AsyncSubjectTest.class,
            rx.subjects.BehaviorSubjectTest.class,
            rx.subjects.BufferUntilSubscriberTest.class,
            rx.subjects.PublishSubjectTest.class,
            rx.subjects.ReplaySubjectBoundedConcurrencyTest.class,
            rx.subjects.ReplaySubjectConcurrencyTest.class,
            rx.subjects.ReplaySubjectTest.class,
            rx.subjects.SerializedSubjectTest.class,
            rx.subjects.TestSubjectTest.class,
            rx.subscriptions.CompositeSubscriptionTest.class,
            rx.subscriptions.MultipleAssignmentSubscriptionTest.class,
            rx.subscriptions.RefCountSubscriptionTest.class,
            rx.subscriptions.SubscriptionsTest.class,
            rx.test.TestObstructionDetectionTest.class,
            rx.util.AssertObservableTest.class,
            rx.EventStreamTest.class,
            rx.SchedulerWorkerTest.class,
            rx.internal.operators.CompletableFromEmitterTest.class,
            rx.internal.operators.DeferredScalarSubscriberTest.class,
            rx.internal.operators.OnSubscribeCollectTest.class,
            rx.internal.operators.OnSubscribeDoOnEachTest.class,
            rx.internal.operators.OnSubscribeFilterTest.class,
            rx.internal.operators.OnSubscribeFromAsyncEmitterTest.class,
            rx.internal.operators.OnSubscribeFromEmitterTest.class,
            rx.internal.operators.OnSubscribeMapTest.class,
            rx.internal.operators.OnSubscribeReduceTest.class,
            rx.internal.operators.OnSubscribeToMapTest.class,
            rx.internal.operators.OnSubscribeToMultimapTest.class,
            rx.internal.operators.OperatorCountTest.class,
            rx.internal.operators.OperatorZipTest.class,
            rx.internal.operators.InternalSafeSubscriberTest.class,
            rx.internal.operators.SingleOperatorZipTest.class,
            rx.internal.producers.ProducerArbiterTest.class,
            rx.internal.producers.ProducerObserverArbiterTest.class,
            rx.internal.producers.SingleDelayedProducerTest.class,
            rx.internal.producers.SingleProducerTest.class,
            rx.internal.schedulers.ExecutorSchedulerTest.class,
            rx.internal.schedulers.InternalGenericScheduledExecutorServiceTest.class,
            rx.internal.util.unsafe.Pow2Test.class,
            rx.internal.util.unsafe.UnsafeAccessTest.class,
            rx.internal.util.ExceptionUtilsTest.class,
            rx.internal.util.PlatformDependentTest.class,
            rx.internal.util.UtilityFunctionsTest.class,
            rx.observables.AsyncOnSubscribeTest.class,
            rx.observables.SyncOnSubscribeTest.class,
            rx.observers.AsyncCompletableSubscriberTest.class,
            rx.observers.CompletableSubscriberTest.class,
            rx.observers.SerializedObserverTest.class,
            rx.schedulers.DeprecatedSchedulersTest.class,
            rx.schedulers.SchedulerLifecycleTest.class,
            rx.schedulers.SchedulerWhenTest.class,
            rx.schedulers.TimeXTest.class,
            rx.subscriptions.SerialSubscriptionTests.class,
            rx.CompletableTest.class,
    };

    public static Class[] bigmem = new Class[] {
//            rx.doppl.ReflectionTest.class,
//            rx.BackpressureTests.class,
//            rx.internal.operators.CachedObservableTest.class,

//            rx.internal.operators.OnSubscribeCombineLatestTest.class, //Still leaking a couple megs

//            rx.internal.operators.OnSubscribeFlattenIterableTest.class,
//            rx.internal.operators.OperatorFlatMapTest.class,
//            rx.internal.operators.OperatorGroupByTest.class,
//            rx.internal.operators.OperatorMergeMaxConcurrentTest.class,
//            rx.internal.operators.OperatorMergeTest.class,
//            rx.internal.operators.OperatorObserveOnTest.class,
//            rx.internal.operators.OperatorPublishTest.class,
//            rx.internal.operators.OperatorReplayTest.class,



//            rx.internal.operators.OperatorRetryTest.class,
//            rx.internal.operators.OperatorSwitchTest.class,
//            rx.internal.operators.OperatorTakeLastTest.class,
//            rx.internal.operators.OperatorTakeLastTimedTest.class,
            rx.internal.util.JCToolsQueueTests.class,

            rx.internal.util.IndexedRingBufferTest.class,
//            rx.subjects.BehaviorSubjectTest.class,

            rx.subjects.ReplaySubjectConcurrencyTest.class,
            ReplaySubjectBoundedConcurrencyTest.class,

            rx.internal.operators.OperatorZipTest.class,

            rx.internal.schedulers.ExecutorSchedulerTest.class,

            rx.observables.SyncOnSubscribeTest.class,
            rx.internal.operators.OperatorDelayTest.class,

//            rx.internal.operators.OnSubscribeCollectTest.class,
    };

    public static Class[] failing = new Class[]{
            //testUnSubscribeForScheduler for both. Probably with 'unsubscribe'
            rx.schedulers.NewThreadSchedulerTest.class,
            rx.schedulers.IoSchedulerTest.class,
            rx.schedulers.ImmediateSchedulerTest.class,


            //This is huge and runs forever. It fails on release pool
            //Maybe a stack overflow, but thread stack doesn't show as that.
            rx.internal.operators.OperatorSwitchTest.class,
    };
}
