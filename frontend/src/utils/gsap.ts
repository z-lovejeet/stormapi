import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import { useGSAP } from '@gsap/react';

gsap.registerPlugin(ScrollTrigger, useGSAP);

// Respect reduced motion preference
if (typeof window !== 'undefined') {
  const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  if (prefersReducedMotion.matches) {
    gsap.globalTimeline.timeScale(20); // Near-instant animations
  }
}

export { gsap, ScrollTrigger, useGSAP };
