Raspberry Pi setup
------------------

0. [Install Raspbian](https://www.raspberrypi.org/downloads/raspbian/) - tested on Jessie
1. `git clone https://github.com/palfrey/pyrexia.git`
2. `cd pyrexia`
3. `sudo pip install -r requirements.txt`
4. &lt;copy config.json&gt;
5. `python generate_config.py`
6. `cd node/raspi`
7. `sudo apt-get install python-dev`
8. `sudo pip install -r requirements.txt`
9. `sudo ln -s ~/pyrexia/node/raspi/init-script sensor`
10. `sudo update-rc.d sensor defaults`
11. `sudo service sensor start`
