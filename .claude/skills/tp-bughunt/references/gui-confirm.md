# Confirming a bug in the real GUI

The harness proves the logic misbehaves; the PE report must show what the
**user** sees. Confirm every kept bug in the GUI and capture a screenshot.

## Never use the user's display

On the user's machine `:1` is their real desktop. Start a private Xvfb on a high
display number as a **background** Bash task, and verify it is Xvfb before
launching anything:

```sh
Xvfb :143 -screen 0 1400x900x24          # run_in_background: true
ps -o pid=,args= -C Xvfb                 # a real Xvfb process (not your own grep)
ls -l /tmp/.X11-unix/                    # an X143 socket
DISPLAY=:143 xdpyinfo | head -1          # "name of display: :143"
```

Never `pkill -f` a pattern that matches your own command line; kill Xvfb by PID.

## Launch inside the sandbox

```sh
<skill>/scripts/sandbox-run.sh <rundir> --display :143 -- java -jar target.jar   # background
DISPLAY=:143 xdotool search --sync --onlyvisible --pid <java pid> | head -1     # window id
DISPLAY=:143 import -window <id> <workdir>/screens/NN-before.png
```

Inside bwrap the java PID differs from the host's view; find the window by
name instead if `--pid` fails: `xdotool search --sync --onlyvisible --name '.'`.

## Typing commands

`xdotool type` races and concatenates keystrokes. Prefer setting up state by
seeding `<rundir>/data/*.json` (copy the one the harness produced) and
relaunching, then type **one** command slowly:

```sh
DISPLAY=:143 xdotool windowactivate --sync <id>
DISPLAY=:143 xdotool type --delay 40 -- '<command>'
DISPLAY=:143 xdotool key Return
sleep 1   # use a Monitor/until-loop if the harness forbids sleep
DISPLAY=:143 import -window <id> <workdir>/screens/NN-after.png
```

For a very long input, put it on the clipboard instead of typing it:

```sh
printf '%s' "$payload" | DISPLAY=:143 xclip -selection clipboard
DISPLAY=:143 xdotool key ctrl+v Return
```

Look at the screenshot (Read the PNG) before writing "Actual". If the GUI
shows nothing wrong while the harness reported a CRASH, the visible symptom
is "the command does nothing and gives no feedback": say exactly that.

If the GUI cannot be driven at all, say so in the report draft and ask the
user to take the screenshot by hand — do not invent the visible outcome.
