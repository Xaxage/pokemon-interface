import { useState } from 'react';

/** Shows the sprite, or a neutral placeholder when PokeAPI has none or it fails to load. */
export function PokemonSprite({ src, alt, size = 96 }: { src: string | null; alt: string; size?: number }) {
  const [failed, setFailed] = useState(false);

  if (!src || failed) {
    return (
      <div className="sprite sprite--missing" style={{ width: size, height: size }} role="img" aria-label={`${alt} (no image)`}>
        ?
      </div>
    );
  }
  return (
    <img
      className="sprite"
      src={src}
      alt={alt}
      width={size}
      height={size}
      loading="lazy"
      decoding="async"
      onError={() => setFailed(true)}
    />
  );
}
