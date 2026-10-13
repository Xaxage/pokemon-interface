/** "mr-mime" → "Mr Mime" */
export function displayName(slug: string): string {
  return slug
    .split('-')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

/** 25 → "#0025" */
export function dexNumber(id: number): string {
  return `#${String(id).padStart(4, '0')}`;
}

/** PokeAPI gives weight in hectograms: 69 hg → "6.9 kg". */
export function weightInKg(hectograms: number | null): string {
  return hectograms == null
    ? '—'
    : `${(hectograms / 10).toLocaleString(undefined, { maximumFractionDigits: 1 })} kg`;
}
