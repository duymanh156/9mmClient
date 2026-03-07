package dev.ninemmteam.api.utils.math;

public class Timer {
   private long startTime = System.currentTimeMillis();
   private long delay = 0;
   private boolean paused = false;

   public Timer() {
   }

   public Timer(long delay) {
      this.delay = delay;
   }

   public boolean passed() {
      if (delay <= 1 && !paused) {
         return true;
      }
      return !paused && System.currentTimeMillis() - startTime >= delay;
   }

   public boolean passed(long ms) {
      if (ms <= 1 && !paused) {
         return true;
      }
      return !paused && System.currentTimeMillis() - startTime >= ms;
   }

   public long getMs() {
      return System.currentTimeMillis() - startTime;
   }

   public boolean passedS(double s) {
      return this.passed((long)s * 1000L);
   }

   public boolean passedMs(double ms) {
      return this.passed((long)ms);
   }

   public void setMs(long ms) {
      this.startTime = System.currentTimeMillis() - ms;
   }

   public void reset() {
      long current = System.currentTimeMillis();
      startTime = current;
   }

   public void resetDelay() {
      long current = System.currentTimeMillis();
      startTime = current;
   }

   public void setDelay(long delay) {
      this.delay = delay;
   }

   public long getDelay() {
      return delay;
   }

   public void setPaused(boolean paused) {
      this.paused = paused;
   }

   public boolean isPaused() {
      return paused;
   }

   public long getStartTime() {
      return startTime;
   }

   public void setCPS(float cps) {
      long delay = (long) (1000 / cps);
      if (cps == 20.0) delay = 0;
      this.setDelay(delay - 10);
   }
}
