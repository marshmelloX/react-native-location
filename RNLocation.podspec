require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|

  s.name           = 'RNLocation'
  s.version        = package['version']
  s.summary        = "React Native Location component for iOS + Android"
  s.authors        = { "Ki Hyun Won" => "kithwon@gmail.com" }
  s.license        = "MIT"
  s.homepage       = "https://github.com/marshmelloX/react-native-location#readme"
  s.platform       = :ios, '7.0'
  s.source         = { :git => "https://github.com/marshmelloX/react-native-location.git", :tag => "v#{s.version}"}
  s.source_files  = "ios/*.{h,m}"

  s.dependency 'React'

end
