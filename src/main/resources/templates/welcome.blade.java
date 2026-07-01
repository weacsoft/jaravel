<!DOCTYPE html>
<html>
<head>
    <title>{{ $title }}</title>
</head>
<body>
    <h1>{{ $title }}</h1>
    <div>Version: {{ $version }}</div>
    <ul>
    @foreach($features as $feature)
        <li>{{ $feature }}</li>
    @endforeach
    </ul>
</body>
</html>
