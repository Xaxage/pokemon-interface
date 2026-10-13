/** Shown instead of "Save to collection" once a Pokémon is already in the collection. */
export function InCollectionBadge() {
  return (
    <span className="in-collection">
      <span aria-hidden>✓</span> In your collection
    </span>
  );
}
