import { useState, type KeyboardEvent } from 'react';

interface TagInputProps {
  id: string;
  tags: string[];
  max: number;
  maxLength: number;
  onChange: (tags: string[]) => void;
  invalid?: boolean;
  describedBy?: string;
}

/** Type a tag and press Enter or comma to add it; Backspace on an empty field removes the last one. */
export function TagInput({ id, tags, max, maxLength, onChange, invalid, describedBy }: TagInputProps) {
  const [draft, setDraft] = useState('');
  const full = tags.length >= max;

  const add = () => {
    const tag = draft.trim();
    if (!tag || full || tags.includes(tag)) return;
    onChange([...tags, tag]);
    setDraft('');
  };

  const onKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault();
      add();
    } else if (event.key === 'Backspace' && draft === '' && tags.length > 0) {
      onChange(tags.slice(0, -1));
    }
  };

  return (
    <div className={`tag-input ${invalid ? 'tag-input--invalid' : ''}`}>
      {tags.map((tag) => (
        <span key={tag} className="tag">
          {tag}
          <button
            type="button"
            className="tag__remove"
            aria-label={`Remove tag ${tag}`}
            onClick={() => onChange(tags.filter((other) => other !== tag))}
          >
            ×
          </button>
        </span>
      ))}
      <input
        id={id}
        value={draft}
        maxLength={maxLength}
        disabled={full}
        placeholder={full ? `Up to ${max} tags` : 'Add a tag…'}
        aria-describedby={describedBy}
        onChange={(event) => setDraft(event.target.value)}
        onKeyDown={onKeyDown}
        onBlur={add}
      />
    </div>
  );
}
