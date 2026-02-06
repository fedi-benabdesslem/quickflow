
import sys
import os
from dotenv import load_dotenv

# Force load dotenv
load_dotenv()

print("--- STARTING TEST ---", file=sys.stderr)
print(f"CWD: {os.getcwd()}", file=sys.stderr)

try:
    print("Importing diarizer...", file=sys.stderr)
    from diarization import diarizer
    print(f"Diarizer imported: {diarizer}", file=sys.stderr)
    
    print("Calling load_model()...", file=sys.stderr)
    diarizer.load_model()
    print("load_model() returned.", file=sys.stderr)
    
    print(f"Is loaded? {diarizer.is_loaded()}", file=sys.stderr)

except Exception as e:
    print(f"CRASH: {e}", file=sys.stderr)
    import traceback
    traceback.print_exc()

print("--- END TEST ---", file=sys.stderr)
