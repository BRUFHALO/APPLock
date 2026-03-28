import Fuse from "fuse.js";
import { useMemo, useCallback } from "react";
import type { VoiceCommand } from "@/types";

interface MatchResult {
  command: VoiceCommand;
  score: number;
}

export function useFuzzyMatcher(commands: VoiceCommand[], threshold: number = 0.6) {
  const fuse = useMemo(() => {
    return new Fuse(commands, {
      keys: ["phrase"],
      threshold: threshold,
      includeScore: true,
      minMatchCharLength: 2,
    });
  }, [commands, threshold]);

  const findMatch = useCallback((input: string): MatchResult | null => {
    if (!input.trim() || commands.length === 0) return null;

    const results = fuse.search(input);
    
    if (results.length > 0 && results[0].score !== undefined) {
      const bestMatch = results[0];
      return {
        command: bestMatch.item,
        score: 1 - (bestMatch.score || 0),
      };
    }

    return null;
  }, [fuse, commands]);

  const findSimilarCommands = useCallback((phrase: string, excludeId?: string): VoiceCommand[] => {
    const results = fuse.search(phrase);
    return results
      .filter(r => r.item.id !== excludeId)
      .slice(0, 3)
      .map(r => r.item);
  }, [fuse]);

  return {
    findMatch,
    findSimilarCommands,
  };
}
