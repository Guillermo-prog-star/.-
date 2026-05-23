"""
combine_questions.py
Combina questions_part1..4.json en un solo questions-bank-v2.json
"""
import json, os, collections

BASE = os.path.dirname(os.path.abspath(__file__))

parts = [
    os.path.join(BASE, f"questions_part{i}.json")
    for i in range(1, 5)
]

all_questions = []
for path in parts:
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    all_questions.extend(data["questions"])

# Verificar IDs unicos
ids = [q["id"] for q in all_questions]
dupes = [k for k,v in collections.Counter(ids).items() if v > 1]
if dupes:
    print(f"WARN: IDs duplicados ({len(dupes)}): {dupes[:5]}")
else:
    print("OK: Todos los IDs son unicos")

# Contar por tipo
by_type = collections.Counter(q["type"] for q in all_questions)
by_milestone = collections.Counter(q["milestoneCode"] for q in all_questions)
by_pillar = collections.Counter(q["pillarName"] for q in all_questions)

print(f"\nTotal preguntas: {len(all_questions)}")
print(f"Por tipo: {dict(sorted(by_type.items()))}")
print(f"Por milestone: {dict(sorted(by_milestone.items()))}")
print(f"Por pilar: {dict(by_pillar.items())}")

out = {
    "version": "2.0",
    "totalCount": len(all_questions),
    "generatedAt": "2026-05-22",
    "typeDistribution": dict(by_type),
    "milestoneDistribution": dict(by_milestone),
    "pillarDistribution": dict(by_pillar),
    "questions": all_questions
}

out_path = os.path.join(BASE, "questions-bank-v2.json")
with open(out_path, "w", encoding="utf-8") as f:
    json.dump(out, f, ensure_ascii=False, indent=2)

print(f"\nOK Banco combinado -> {out_path}")
