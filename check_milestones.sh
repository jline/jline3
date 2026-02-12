#!/bin/bash
set -e

# Configuration
REPO="jline/jline3"
BRANCH="jline-3.x"
REQUIRED_MILESTONE="3.30.7"
AUTO_FIX=${AUTO_FIX:-false}  # Set to 'true' to automatically add milestones

echo "Vérification des PRs sur $REPO ($BRANCH) depuis la dernière release..."

# 1. Récupérer les infos de la dernière release
echo "Récupération de la dernière release..."
LATEST_RELEASE_JSON=$(gh api "repos/$REPO/releases/latest")
LATEST_TAG=$(echo "$LATEST_RELEASE_JSON" | jq -r .tag_name)
LATEST_DATE=$(echo "$LATEST_RELEASE_JSON" | jq -r .published_at)

if [ -z "$LATEST_TAG" ] || [ "$LATEST_TAG" == "null" ]; then
  echo "Erreur: Impossible de trouver la dernière release."
  exit 1
fi

echo "Dernière release : $LATEST_TAG (publiée le $LATEST_DATE)"

# 1b. Récupérer l'ID du milestone requis
echo "Récupération de l'ID du milestone '$REQUIRED_MILESTONE'..."
MILESTONE_ID=$(gh api "repos/$REPO/milestones" --jq ".[] | select(.title == \"$REQUIRED_MILESTONE\") | .number")

if [ -z "$MILESTONE_ID" ]; then
  echo "Erreur: Milestone '$REQUIRED_MILESTONE' introuvable dans le repo."
  echo "Milestones disponibles:"
  gh api "repos/$REPO/milestones" --jq '.[].title'
  exit 1
fi

echo "Milestone '$REQUIRED_MILESTONE' trouvé avec l'ID: $MILESTONE_ID"

# 2. Lister les PRs mergées depuis cette date
echo "Récupération des PRs mergées..."
PRS_JSON=$(gh pr list \
  --repo "$REPO" \
  --base "$BRANCH" \
  --search "merged:>$LATEST_DATE" \
  --state merged \
  --limit 100 \
  --json number,title,url,milestone)

rm -f .pr_check_failed
rm -f .prs_to_fix
echo ""
echo "--- Résultats de la vérification ---"

# 3. Vérifier les milestones
echo "$PRS_JSON" | jq -r '.[] | @base64' | while read -r row; do
  _jq() {
    echo "${row}" | base64 --decode | jq -r "${1}"
  }

  PR_NUM=$(_jq '.number')
  PR_TITLE=$(_jq '.title')
  PR_URL=$(_jq '.url')
  PR_MILESTONE=$(_jq '.milestone.title // empty')

  if [ "$PR_MILESTONE" == "$REQUIRED_MILESTONE" ]; then
    echo "[OK] PR #$PR_NUM: $PR_TITLE"
  else
    echo "[FAIL] PR #$PR_NUM: $PR_TITLE"
    if [ -z "$PR_MILESTONE" ]; then
      echo " => Milestone MANQUANT (Attendu: $REQUIRED_MILESTONE)"
    else
      echo " => Milestone trouvé: '$PR_MILESTONE' (Attendu: $REQUIRED_MILESTONE)"
    fi
    echo " => $PR_URL"
    echo "$PR_NUM" >> .prs_to_fix
    touch .pr_check_failed
  fi
done

# 4. Corriger les milestones si AUTO_FIX est activé
if [ -f .prs_to_fix ]; then
  PRS_TO_FIX=$(cat .prs_to_fix | wc -l)
  echo ""
  
  if [ "$AUTO_FIX" == "true" ]; then
    echo "🔧 Correction automatique activée - Ajout du milestone aux $PRS_TO_FIX PRs..."
    
    while read -r PR_NUM; do
      echo "Ajout du milestone '$REQUIRED_MILESTONE' à la PR #$PR_NUM..."
      if gh api \
        --method PATCH \
        "repos/$REPO/issues/$PR_NUM" \
        -f milestone="$MILESTONE_ID" > /dev/null 2>&1; then
        echo "✅ PR #$PR_NUM mise à jour avec succès"
      else
        echo "❌ Erreur lors de la mise à jour de la PR #$PR_NUM"
      fi
    done < .prs_to_fix
    
    rm -f .prs_to_fix .pr_check_failed
    echo ""
    echo "✅ Correction terminée avec succès!"
    exit 0
  else
    echo "ℹ️  Pour corriger automatiquement, relancez avec: AUTO_FIX=true $0"
    echo "   Ou exécutez manuellement:"
    while read -r PR_NUM; do
      echo "   gh api --method PATCH repos/$REPO/issues/$PR_NUM -f milestone=$MILESTONE_ID"
    done < .prs_to_fix
  fi
  
  rm -f .prs_to_fix
fi

# 5. Résumé final
PR_COUNT=$(echo "$PRS_JSON" | jq '. | length')

if [ -f .pr_check_failed ]; then
  rm .pr_check_failed
  echo ""
  echo "❌ ÉCHEC : Certaines PRs n'ont pas le bon milestone."
  exit 1
else
  echo ""
  if [ "$PR_COUNT" -eq 0 ]; then
    echo "Aucune PR mergée depuis $LATEST_TAG."
  else
    echo "✅ SUCCÈS : Les $PR_COUNT PRs ont bien le milestone '$REQUIRED_MILESTONE'."
  fi
  exit 0
fi
