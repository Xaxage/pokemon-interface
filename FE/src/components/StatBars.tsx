import type { Stat } from '../api/types';

const LABELS: Record<string, string> = {
  hp: 'HP',
  attack: 'Attack',
  defense: 'Defense',
  'special-attack': 'Sp. Atk',
  'special-defense': 'Sp. Def',
  speed: 'Speed',
};

/** Highest base stat in the games (Blissey's HP), so bars compare across Pokémon. */
const MAX_BASE = 255;

export function StatBars({ stats }: { stats: Stat[] }) {
  if (stats.length === 0) return <p className="muted">No stats available.</p>;
  const total = stats.reduce((sum, stat) => sum + stat.base, 0);

  return (
    <table className="stats">
      <tbody>
        {stats.map((stat) => (
          <tr key={stat.name}>
            <th scope="row">{LABELS[stat.name] ?? stat.name}</th>
            <td className="stats__value">{stat.base}</td>
            <td className="stats__bar-cell">
              <div className="stats__bar" aria-hidden>
                <span style={{ width: `${Math.min(stat.base / MAX_BASE, 1) * 100}%` }} />
              </div>
            </td>
          </tr>
        ))}
        <tr className="stats__total">
          <th scope="row">Total</th>
          <td className="stats__value">{total}</td>
          <td />
        </tr>
      </tbody>
    </table>
  );
}
