# ============================================================
#  SOLVEX LEVEL GENERATOR — coller dans une cellule Colab
#  820 niveaux, ordre aléatoire, aucune catégorie visible
# ============================================================

import random, json, time
from google.colab import files

# ── Constantes ──────────────────────────────────────────────
EMPTY, FIRE, WATER = 0, 1, 2
CELL_NAMES = {EMPTY: 'EMPTY', FIRE: 'FIRE', WATER: 'WATER'}

# ── Générateur de solution ───────────────────────────────────
def is_ok(grid, size, r, c):
    el = grid[r][c]; half = size // 2
    if grid[r].count(el) > half: return False
    if sum(1 for i in range(r + 1) if grid[i][c] == el) > half: return False
    if c >= 2 and grid[r][c-1] == el and grid[r][c-2] == el: return False
    if r >= 2 and grid[r-1][c] == el and grid[r-2][c] == el: return False
    return True

def _fill(grid, size, pos, rng):
    if pos == size * size: return True
    r, c = divmod(pos, size)
    options = [FIRE, WATER]; rng.shuffle(options)
    for el in options:
        grid[r][c] = el
        if is_ok(grid, size, r, c) and _fill(grid, size, pos + 1, rng): return True
    grid[r][c] = EMPTY
    return False

def build_solution(size, seed):
    for offset in range(5):
        rng = random.Random(seed + offset)
        grid = [[EMPTY] * size for _ in range(size)]
        if _fill(grid, size, 0, rng): return grid
    return None

# ── Contraintes ──────────────────────────────────────────────
def build_constraints(sol, size, n_constraints, rng):
    pairs = []
    for r in range(size):
        for c in range(size):
            if c+1 < size: pairs.append(((r,c),(r,c+1)))
            if r+1 < size: pairs.append(((r,c),(r+1,c)))
    rng.shuffle(pairs)
    return [{'r1':r1,'c1':c1,'r2':r2,'c2':c2,
             'type':'EQUAL' if sol[r1][c1]==sol[r2][c2] else 'DIFFERENT'}
            for (r1,c1),(r2,c2) in pairs[:n_constraints]]

# ── Masquage ─────────────────────────────────────────────────
def mask_solution(sol, size, reveal_ratio, rng):
    cells = [(r,c) for r in range(size) for c in range(size)]
    rng.shuffle(cells)
    revealed = set(cells[:int(size * size * reveal_ratio)])
    return [[sol[r][c] if (r,c) in revealed else EMPTY for c in range(size)]
            for r in range(size)]

# ── Solveur d'unicité ────────────────────────────────────────
def _is_ok_solver(grid, size, r, c):
    el = grid[r][c]
    if el == EMPTY: return True
    half = size // 2
    if grid[r].count(el) > half: return False
    if sum(1 for i in range(size) if grid[i][c] == el) > half: return False
    for dc in (-2,-1,0):
        c0 = c+dc
        if 0<=c0 and c0+2<size and grid[r][c0]==el and grid[r][c0+1]==el and grid[r][c0+2]==el:
            return False
    for dr in (-2,-1,0):
        r0 = r+dr
        if 0<=r0 and r0+2<size and grid[r0][c]==el and grid[r0+1][c]==el and grid[r0+2][c]==el:
            return False
    return True

def count_solutions(clues, size, constraints, limit=2):
    grid = [row[:] for row in clues]
    empty_cells = [(r,c) for r in range(size) for c in range(size) if grid[r][c]==EMPTY]
    found = [0]
    cc = {}
    for con in constraints:
        for k in ((con['r1'],con['c1']),(con['r2'],con['c2'])):
            cc.setdefault(k,[]).append(con)
    def check(r, c):
        for con in cc.get((r,c),[]):
            e1=grid[con['r1']][con['c1']]; e2=grid[con['r2']][con['c2']]
            if e1==EMPTY or e2==EMPTY: continue
            if con['type']=='EQUAL' and e1!=e2: return False
            if con['type']=='DIFFERENT' and e1==e2: return False
        return True
    def bt(idx):
        if found[0] >= limit: return
        if idx == len(empty_cells): found[0]+=1; return
        r, c = empty_cells[idx]
        for el in (FIRE, WATER):
            grid[r][c] = el
            if _is_ok_solver(grid, size, r, c) and check(r, c): bt(idx+1)
            if found[0] >= limit: break
        grid[r][c] = EMPTY
    bt(0)
    return found[0]

# ── Génération d'un niveau ───────────────────────────────────
def generate_level(level_num, size, n_constraints, reveal_ratio, seed):
    rng = random.Random(seed)
    sol = build_solution(size, seed)
    if sol is None: return None
    constraints = build_constraints(sol, size, n_constraints, rng)
    clues = mask_solution(sol, size, reveal_ratio, rng)
    if count_solutions(clues, size, constraints, 2) != 1: return None
    return {
        'id':          level_num,
        'levelNumber': level_num,
        'size':        size,
        'clues':       clues,
        'constraints': constraints,
        'solution':    sol,
    }

# ── Configuration des groupes de génération ──────────────────
# (size, n_constraints, reveal_ratio, count)
# Tous les niveaux sont en 6x6, difficulté croissante par constraints/ratio.
# Après génération, tous les niveaux sont MÉLANGÉS aléatoirement.
CONFIGS = [
    (6,  2, 0.50, 100),   # niveaux très faciles
    (6,  2, 0.45, 100),   # niveaux faciles
    (6,  3, 0.42, 100),   # niveaux faciles-moyens
    (6,  3, 0.38, 100),   # niveaux moyens
    (6,  4, 0.35, 100),   # niveaux moyens-difficiles
    (6,  4, 0.30, 120),   # niveaux difficiles
    (6,  5, 0.27, 100),   # niveaux très difficiles
    (6,  6, 0.24, 100),   # niveaux experts
]
TARGET = 820
SHUFFLE_SEED = 42  # changer ce nombre pour obtenir un ordre différent

print(f"Génération de {TARGET} niveaux...")
print()

levels = []; level_num = 1; seed = 200_000; t_total = time.time()

for i, (size, n_con, ratio, count) in enumerate(CONFIGS):
    done = 0; attempts = 0; t0 = time.time()
    while done < count:
        lvl = generate_level(level_num, size, n_con, ratio, seed)
        seed += 1; attempts += 1
        if lvl:
            levels.append(lvl); level_num += 1; done += 1
            if done % 40 == 0 or done == count:
                rate = done / (time.time()-t0) if time.time()-t0 > 0 else 0
                print(f"  Groupe {i+1}/8 [{size}x{size}]: {done}/{count}  ({rate:.0f}/s)")
        if attempts > count * 60: break
    print(f"  Groupe {i+1}: {done} niveaux en {time.time()-t0:.1f}s")

# Compléter jusqu'à TARGET si nécessaire (6x6 expert)
while len(levels) < TARGET:
    lvl = generate_level(level_num, 6, 6, 0.24, seed)
    seed += 1
    if lvl:
        levels.append(lvl); level_num += 1

print(f"\n{len(levels)} niveaux générés en {time.time()-t_total:.1f}s")

# ── MÉLANGE ALÉATOIRE de tous les niveaux ────────────────────
print(f"\nMélange aléatoire des niveaux (seed={SHUFFLE_SEED})...")
random.Random(SHUFFLE_SEED).shuffle(levels)

# Renuméroter 1 → N après mélange
for i, lvl in enumerate(levels):
    lvl['id'] = i + 1
    lvl['levelNumber'] = i + 1

print(f"Ordre mélangé — {len(levels)} niveaux 6×6")

# ── Validation ───────────────────────────────────────────────
print("\nValidation...")
errors = 0
for lvl in levels:
    size=lvl['size']; sol=lvl['solution']; half=size//2
    for r in range(size):
        if sol[r].count(FIRE)!=half or sol[r].count(WATER)!=half: errors+=1
    for c in range(size):
        col=[sol[r][c] for r in range(size)]
        if col.count(FIRE)!=half: errors+=1
    for r in range(size):
        for c in range(size-2):
            if sol[r][c]==sol[r][c+1]==sol[r][c+2]: errors+=1
    for c in range(size):
        for r in range(size-2):
            if sol[r][c]==sol[r+1][c]==sol[r+2][c]: errors+=1
    for r in range(size):
        for c in range(size):
            if lvl['clues'][r][c]!=EMPTY and lvl['clues'][r][c]!=sol[r][c]: errors+=1
    if count_solutions(lvl['clues'],size,lvl['constraints'],2)!=1: errors+=1

if errors == 0:
    print(f"✅ {len(levels)} niveaux CORRECTS, solutions uniques, ordre aléatoire !")
else:
    print(f"❌ {errors} erreurs détectées")

# ── Export JSON ──────────────────────────────────────────────
print("\nSauvegarde...")

# JSON compact (nombres 0/1/2) — à copier dans app/src/main/assets/
with open('solvex_levels_compact.json', 'w') as f:
    json.dump({'version':1, 'total':len(levels), 'levels':levels}, f, separators=(',',':'))
print(f"  solvex_levels_compact.json  → {len(json.dumps(levels))//1024} KB")
print(f"  ⚠️  Copier ce fichier dans : app/src/main/assets/solvex_levels_compact.json")

# JSON lisible (EMPTY/FIRE/WATER) — pour debug / lecture humaine
named = [
    {**lvl,
     'clues':    [[CELL_NAMES[v] for v in row] for row in lvl['clues']],
     'solution': [[CELL_NAMES[v] for v in row] for row in lvl['solution']]}
    for lvl in levels
]
with open('solvex_levels.json', 'w') as f:
    json.dump({'version':1, 'total':len(levels), 'levels':named}, f, separators=(',',':'))
print(f"  solvex_levels.json          → {len(json.dumps(named))//1024} KB")

# ── Téléchargement ────────────────────────────────────────────
print("\nTéléchargement...")
files.download('solvex_levels_compact.json')
files.download('solvex_levels.json')
print("✅ Terminé !")

# ── Résumé ────────────────────────────────────────────────────
from collections import Counter
size_cnt = Counter(lvl['size'] for lvl in levels)
print(f"\n── Résumé ──────────────────────────────────────────")
print(f"  Total  : {len(levels)} niveaux 6×6 (ordre aléatoire)")
print(f"  Aucune catégorie — tous dans une seule liste")
