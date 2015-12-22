apt_package 'python-pip'
apt_package 'python-dev'

cookbook_file "requirements.txt" do
	source "raspi/requirements.txt"
end

execute "pip upgrade" do
	command "pip install --upgrade pip"
end

execute "pip install" do
	command "pip install -r requirements.txt"
end

cookbook_file "/etc/init.d/sensor" do
	source "raspi/init-script"
	mode '0755'
end

service "sensor" do
    supports :status => true, :restart => true, :reload => true
    action [ :enable, :start ]
    provider Chef::Provider::Service::Init::Debian
end
