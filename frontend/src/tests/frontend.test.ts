import { describe, it, expect } from 'vitest';
import { formatBytes } from '../api';

describe('Frontend Logic & Validation Tests', () => {
  it('formats byte numbers into human-readable strings correctly', () => {
    expect(formatBytes(0)).toBe('0 B');
    expect(formatBytes(1024)).toBe('1 KB');
    expect(formatBytes(1048576)).toBe('1 MB');
    expect(formatBytes(52428800)).toBe('50 MB');
    expect(formatBytes(1073741824)).toBe('1 GB');
  });

  it('validates URL formatting logic', () => {
    function validateUrlInput(input: string): { valid: boolean; error?: string; normalized?: string } {
      const trimmed = input.trim();
      if (!trimmed) {
        return { valid: false, error: 'Please enter a website URL.' };
      }
      let testUrl = trimmed;
      if (!testUrl.startsWith('http://') && !testUrl.startsWith('https://')) {
        testUrl = 'https://' + testUrl;
      }
      try {
        const parsed = new URL(testUrl);
        if (!parsed.hostname.includes('.')) {
          return { valid: false, error: 'Please enter a valid website hostname.' };
        }
        return { valid: true, normalized: testUrl };
      } catch {
        return { valid: false, error: 'Invalid URL format.' };
      }
    }

    expect(validateUrlInput('')).toEqual({ valid: false, error: 'Please enter a website URL.' });
    expect(validateUrlInput('topbrains.com')).toEqual({ valid: true, normalized: 'https://topbrains.com' });
    expect(validateUrlInput('https://topbrains.com/')).toEqual({ valid: true, normalized: 'https://topbrains.com/' });
    expect(validateUrlInput('not-a-valid-domain')).toEqual({ valid: false, error: 'Please enter a valid website hostname.' });
  });

  it('verifies breakdown aggregation', () => {
    const discovered = [
      { url: 'https://topbrains.com/app.js', resourceType: 'SCRIPT' },
      { url: 'https://topbrains.com/vendor.js', resourceType: 'SCRIPT' },
      { url: 'https://topbrains.com/style.css', resourceType: 'STYLESHEET' },
      { url: 'https://topbrains.com/logo.png', resourceType: 'IMAGE' }
    ];

    const breakdown: Record<string, number> = {};
    for (const r of discovered) {
      breakdown[r.resourceType] = (breakdown[r.resourceType] || 0) + 1;
    }

    expect(breakdown['SCRIPT']).toBe(2);
    expect(breakdown['STYLESHEET']).toBe(1);
    expect(breakdown['IMAGE']).toBe(1);
  });
});
