package com.android.systemui.recents.misc;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import java.util.ArrayList;

public class ReferenceCountedTrigger {
    Context mContext;
    int mCount;
    Runnable mDecrementRunnable = new Runnable() {
        public void run() {
            ReferenceCountedTrigger.this.decrement();
        }
    };
    Runnable mErrorRunnable;
    ArrayList<Runnable> mFirstIncRunnables = new ArrayList();
    Runnable mIncrementRunnable = new Runnable() {
        public void run() {
            ReferenceCountedTrigger.this.increment();
        }
    };
    ArrayList<Runnable> mLastDecRunnables = new ArrayList();

    public ReferenceCountedTrigger(Context context, Runnable firstIncRunnable, Runnable lastDecRunnable, Runnable errorRunanable) {
        this.mContext = context;
        if (firstIncRunnable != null) {
            this.mFirstIncRunnables.add(firstIncRunnable);
        }
        if (lastDecRunnable != null) {
            this.mLastDecRunnables.add(lastDecRunnable);
        }
        this.mErrorRunnable = errorRunanable;
    }

    public void increment() {
        if (this.mCount == 0 && !this.mFirstIncRunnables.isEmpty()) {
            int numRunnables = this.mFirstIncRunnables.size();
            for (int i = 0; i < numRunnables; i++) {
                ((Runnable) this.mFirstIncRunnables.get(i)).run();
            }
        }
        this.mCount++;
    }

    public void addLastDecrementRunnable(Runnable r) {
        boolean ensureLastDecrement = this.mCount == 0;
        if (ensureLastDecrement) {
            increment();
        }
        this.mLastDecRunnables.add(r);
        if (ensureLastDecrement) {
            decrement();
        }
    }

    public void decrement() {
        this.mCount--;
        if (this.mCount == 0 && !this.mLastDecRunnables.isEmpty()) {
            int numRunnables = this.mLastDecRunnables.size();
            for (int i = 0; i < numRunnables; i++) {
                ((Runnable) this.mLastDecRunnables.get(i)).run();
            }
        } else if (this.mCount >= 0) {
        } else {
            if (this.mErrorRunnable != null) {
                this.mErrorRunnable.run();
                return;
            }
            new Throwable("Invalid ref count").printStackTrace();
            Console.logError(this.mContext, "Invalid ref count");
        }
    }

    public Runnable decrementAsRunnable() {
        return this.mDecrementRunnable;
    }

    public AnimatorListener decrementOnAnimationEnd() {
        return new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                ReferenceCountedTrigger.this.decrement();
            }
        };
    }
}
