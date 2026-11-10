"""Mixed browsing traffic for the PetClinic demo.

Simulates a few users clicking around the clinic: finding owners, opening owner
pages, starting a visit booking and looking at the vets. Uses only the standard
library.

    BASE_URL=http://localhost:8080 WORKERS=4 python3 traffic.py

BASE_URL includes the context path if the app has one (e.g. .../petclinic).
"""

import os
import random
import threading
import time
import urllib.error
import urllib.request

BASE_URL = os.environ.get("BASE_URL", "http://localhost:8080").rstrip("/")
WORKERS = int(os.environ.get("WORKERS", "4"))

# (owner id, pet id) pairs from the H2 seed data
PETS = [(1, 1), (2, 2), (3, 3), (4, 4), (5, 5), (6, 6), (6, 7), (7, 8), (8, 9), (10, 10), (10, 11)]
LAST_NAMES = ["", "Davis", "Franklin", "Black", "Estaban", "Coleman", "McTavish", "Es"]

PAGES = [
    (10, lambda: "/"),
    (15, lambda: "/owners?lastName=" + random.choice(LAST_NAMES)),
    (25, lambda: "/owners/%d" % random.randint(1, 10)),
    (5, lambda: "/owners/find"),
    (10, lambda: "/owners/%d/pets/%d/visits/new" % random.choice(PETS)),
    (15, lambda: "/vets.html"),
    (5, lambda: "/vets.html?page=2"),
    (15, lambda: "/vets"),
]
WEIGHTS = [weight for weight, _ in PAGES]


def fetch(path):
    request = urllib.request.Request(BASE_URL + path, headers={"Accept": "application/json"} if path == "/vets" else {})
    started = time.monotonic()
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            response.read()
            status = response.status
    except urllib.error.HTTPError as error:
        status = error.code
    except (urllib.error.URLError, TimeoutError) as error:
        status = "ERR %s" % error
    return status, (time.monotonic() - started) * 1000


def worker(number):
    while True:
        path = random.choices(PAGES, weights=WEIGHTS)[0][1]()
        status, millis = fetch(path)
        print("%s worker-%d %s %6.0f ms %s" % (time.strftime("%H:%M:%S"), number, status, millis, path), flush=True)
        time.sleep(random.uniform(0.5, 1.5))


if __name__ == "__main__":
    print("Sending traffic to %s with %d worker(s); Ctrl+C to stop" % (BASE_URL, WORKERS), flush=True)
    for n in range(1, WORKERS + 1):
        threading.Thread(target=worker, args=(n,), daemon=True).start()
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        pass
