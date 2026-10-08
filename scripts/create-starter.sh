#!/usr/bin/env bash
# Turns a checkout of this blueprint into a single-stack starter, in place.
#
#   scripts/create-starter.sh kotlin-gradle [--flat]
#   scripts/create-starter.sh java-maven [--flat]
#
# It deletes the other variant with its workflows and strips every block marked for it from the
# configuration and the docs. With --flat it also moves the build to the repo root, so the result
# looks like a plain Spring Boot project.
# Nothing is committed; review the result with `git status`. See docs/starter.md.
set -euo pipefail

usage() { echo "usage: $0 <kotlin-gradle|java-maven> [--flat]" >&2; exit 1; }

case "${1:-}" in
  kotlin-gradle) kept=kotlin-gradle; dropped=java-maven;    dropped_terms='maven|mvnw|checkstyle|mockito' ;;
  java-maven)    kept=java-maven;    dropped=kotlin-gradle; dropped_terms='kotlin|gradle|konsist|mockk' ;;
  *) usage ;;
esac

case "${2:-}" in
  "")     flat=false; dropped_blocks="variant:($dropped|blueprint)" ;;
  --flat) flat=true;  dropped_blocks="variant:($dropped|blueprint|nested)" ;;
  *) usage ;;
esac

cd "$(git rev-parse --show-toplevel)"

if [[ -n "$(git status --porcelain)" ]]; then
  echo "The working tree has uncommitted changes. Commit or discard them first." >&2
  exit 1
fi

marker='^[[:space:]]*(#|<!--) /?variant:'

rewrite() {
  local file=$1 rewritten
  shift
  rewritten=$(mktemp)
  "$@" "$file" > "$rewritten"
  cat "$rewritten" > "$file"
  rm "$rewritten"
}

strip_marked_blocks() {
  awk -v dropped="$dropped_blocks" -v marker="$marker" '
    $0 ~ marker && $0 ~ "/variant:" { skipping = 0; next }
    $0 ~ marker                     { skipping = ($0 ~ dropped); next }
    !skipping
  ' "$1" | cat -s
}

tracked_text_files() {
  git ls-files -- '*.md' '*.yml' '*.toml' '*.json' '*.kts' '*.xml' .gitignore ':!docs/adr' ':!package-lock.json'
}

append_the_variant_readme_to_the_root_readme() {
  local merged
  merged=$(mktemp)
  {
    sed '/^## .*Contributing/,$d' README.md
    sed -e '1,/^## /{/^## /!d;}' -e 's|\.\./||g' "$kept/README.md"
    echo
    sed -n '/^## .*Contributing/,$p' README.md
  } > "$merged"
  cat "$merged" > README.md
  rm "$merged"
  git rm -q -f "$kept/README.md"
}

move_the_build_to_the_repo_root() {
  tracked_text_files | while read -r file; do
    rewrite "$file" sed \
      -e "/working-directory: $kept\$/d" \
      -e "s|cd $kept && ||g" \
      -e "s|\"/$kept\"|\"/\"|g" \
      -e "s|\`$kept/\`|the repo root|g" \
      -e "s|$kept/||g" \
      -e "s|\*/service/app|service/app|g"
  done
  git ls-files -- "$kept" | cut -d/ -f2 | sort -u | while read -r entry; do
    git mv "$kept/$entry" "$entry"
  done
  find "$kept" -type d -empty -delete
}

git rm -r -q "$dropped" ".github/workflows/"*"-$dropped.yml" .github/workflows/blueprint.yml \
  docs/adr/0013-two-stack-variants-side-by-side-on-main.md docs/starter.md scripts/create-starter.sh

git grep -l -E "$marker" | while read -r file; do
  rewrite "$file" strip_marked_blocks
done
rewrite docs/README.md grep -v '0013-two-stack-variants'

if $flat; then
  append_the_variant_readme_to_the_root_readme
  move_the_build_to_the_repo_root
fi
git add -A

echo "Created the $kept starter. Review it with: git status"
if leftovers=$(git grep -n -i -E "$dropped_terms" -- ':!docs/adr' ':!package-lock.json' ":!$kept" ':!service' ':!gradle*' ':!mvnw*' ':!pom.xml' ':!*.kts'); then
  echo
  echo "These lines still mention the removed variant and need a manual look:"
  echo "$leftovers"
fi
