package rx.internal.util;

/**
* Written by Gil Tene of Azul Systems, and released to the public domain,
* as explained at http://creativecommons.org/publicdomain/zero/1.0/
* 
* Originally from https://gist.github.com/giltene/b3e5490c2d7edb232644
* Explained at http://stuff-gil-says.blogspot.com/2014/11/writerreaderphaser-story-about-new.html
*/

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
* Single-Writer Single-Reader Phaser. 
*/
public final class SWSRPhaser {
   private volatile long startEpoch = 0;
   private volatile long evenEndEpoch = 0;
   private volatile long oddEndEpoch = Long.MIN_VALUE;

   private static final AtomicLongFieldUpdater<SWSRPhaser> startEpochUpdater =
           AtomicLongFieldUpdater.newUpdater(SWSRPhaser.class, "startEpoch");
   private static final AtomicLongFieldUpdater<SWSRPhaser> evenEndEpochUpdater =
           AtomicLongFieldUpdater.newUpdater(SWSRPhaser.class, "evenEndEpoch");
   private static final AtomicLongFieldUpdater<SWSRPhaser> oddEndEpochUpdater =
           AtomicLongFieldUpdater.newUpdater(SWSRPhaser.class, "oddEndEpoch");

   public long writerCriticalSectionEnter() {
       return startEpochUpdater.getAndIncrement(this);
   }

   public void writerCriticalSectionExit(long criticalValueAtEnter) {
       if (criticalValueAtEnter < 0) {
           oddEndEpochUpdater.lazySet(this, criticalValueAtEnter + 1);
       } else {
           evenEndEpochUpdater.lazySet(this, criticalValueAtEnter + 1);
       }
   }

   public boolean isEvenPhase() {
       return startEpoch >= 0;
   }
   public boolean isOddPhase() {
       return startEpoch < 0;
   }
   
   public long flipPhase(long yieldTimeNsec) {
       boolean nextPhaseIsEven = (startEpoch < 0); // Current phase is odd...

       long initialStartValue;
       // First, clear currently unused [next] phase end epoch (to proper initial value for phase):
       if (nextPhaseIsEven) {
           initialStartValue = 0;
           evenEndEpochUpdater.lazySet(this, 0);
       } else {
           initialStartValue = Long.MIN_VALUE;
           oddEndEpochUpdater.lazySet(this, Long.MIN_VALUE);
       }

       // Next, reset start value, indicating new phase, and retain value at flip:
       long startValueAtFlip = startEpochUpdater.getAndSet(this, initialStartValue);

       // Now, spin until previous phase end value catches up with start value at flip:
       boolean caughtUp = false;
       do {
           if (nextPhaseIsEven) {
               caughtUp = (oddEndEpoch == startValueAtFlip);
           } else {
               caughtUp = (evenEndEpoch == startValueAtFlip);
           }
           if (!caughtUp) {
               if (yieldTimeNsec == 0) {
                   Thread.yield();
               } else 
               if (yieldTimeNsec > 0) {
                   try {
                       TimeUnit.NANOSECONDS.sleep(yieldTimeNsec);
                   } catch (InterruptedException ex) {
                   }
               }
           }
       } while (!caughtUp);
       
       return startValueAtFlip;
   }
}