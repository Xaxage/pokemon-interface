import { useState } from 'react';
import type { Skill } from '../api/types';
import { displayName } from '../format';

function howLearned(skill: Skill): string {
  return skill.methods
    .map((method) => (method.level ? `${displayName(method.method)} (Lv ${method.level})` : displayName(method.method)))
    .join(', ');
}

/** The Pokémon's moves, collapsed by default; the list is only rendered once opened, as it can run to 100+ entries. */
export function SkillList({ skills }: { skills: Skill[] }) {
  const [open, setOpen] = useState(false);

  if (skills.length === 0) return <p className="muted small">No known skills</p>;

  return (
    <div className="skills">
      <button type="button" className="skills__toggle" aria-expanded={open} onClick={() => setOpen((value) => !value)}>
        {open ? '▾' : '▸'} {skills.length} skill{skills.length === 1 ? '' : 's'}
      </button>
      {open && (
        <ul className="skills__list">
          {skills.map((skill) => (
            <li key={skill.name}>
              <span>{displayName(skill.name)}</span>
              <span className="muted">{howLearned(skill)}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

/** Lowest level the move is learned at by levelling up, or null when it is only learned some other way. */
function levelLearned(skill: Skill): number | null {
  const levels = skill.methods.flatMap((method) => (method.level ? [method.level] : []));
  return levels.length > 0 ? Math.min(...levels) : null;
}

/** Level-up moves first, in the order they are learned, then every other move alphabetically. */
function byHowLearned(a: Skill, b: Skill): number {
  const levelA = levelLearned(a);
  const levelB = levelLearned(b);
  if (levelA !== null && levelB !== null && levelA !== levelB) return levelA - levelB;
  if (levelA !== null && levelB === null) return -1;
  if (levelA === null && levelB !== null) return 1;
  return a.name.localeCompare(b.name);
}

/** Every move in full, for the detail page. */
export function SkillTable({ skills }: { skills: Skill[] }) {
  if (skills.length === 0) return <p className="muted">No skills in the newest games.</p>;

  return (
    <div className="skill-table__scroll">
      <table className="skill-table">
        <thead>
          <tr>
            <th scope="col">Move</th>
            <th scope="col">How it is learned</th>
          </tr>
        </thead>
        <tbody>
          {[...skills].sort(byHowLearned).map((skill) => (
            <tr key={skill.name}>
              <th scope="row">{displayName(skill.name)}</th>
              <td className="muted">{howLearned(skill)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
